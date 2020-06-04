package bdd;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cette classe permet d'enregistrer un tableau associatif dans un fichier.
 * Le tableau assicie une clé de type texte avec une valeur qui implémente l'interface Serializable.
 * <p>
 * Chaque enregistrement est fait dans manière linéraire dans le fichier à travers la fonction {@link #putObject(String, Serializable)}
 * Lors de l'ajout d'un nouvel élément, la fonction va d'abord serialiser l'élément en un tableau binaire.
 * Elle va ensuite rechercher un espace vide contigue suffisement grand.
 * Si cet espace n'est pas trouvé, l'élément est ajouté en fin de fichier.
 * Les 16 premiers octets sont un espace reservé pour stocker des références qui pointes vers des métadonnées décrites plus bas.
 * Une fois l'ajout effectué, le tableau associatif {@link #links} est mise à jour, pour permettre d'associer la clé de l'enregistrement avec la position de l'enregistrement dans le fichier.
 * <p>
 * Pour récupérer l'enregistrement, il faut utiliser la fonction {@link #getObject(String)}.
 * Cette fonction va rechercher la position de l'enregistrement dans le fichier grace au tableau associatif {@link #links},
 * puis elle va déserializer l'enregistrement pour le retourner.
 * <p>
 * La suppression, consiste à appeler la fonction {@link #removeObject(String)} qui va ajouter l'espace occupé par l'engistrement dans le tableau trié {@link #freeSpaceIntervals},
 * sauf si l'enregistrement se trouve en fin de fichier. Dans ce dernier cas, le fichier est tronqué.
 * <p>
 * De même, lors de l'ajout d'un nouvel enregistrement, le tableau {@link #freeSpaceIntervals} est mis à jour si un espace vide contigue est trouvé.
 * <p>
 * A la fermeture du fichier, les deux fonctions suivantes sont appelées :
 * <ul>
 *     <li>{@link #saveLinks()} qui sauvegarde le tableau {@link #links} comme si c'était un entregistrement quelconque, mais sans clé. La position de cet enregistrement dans le fichier est enregistré à la position déterminée par {@link #LINKS_REFERENCE_POSITION} du même fichier.</li>
 *     <li>{@link #saveFreeSpaceTab()} ()} qui sauvegarde le tableau {@link #freeSpaceIntervals} comme si c'était un enregistrement quelconque aussi. La position de cet enregistrement dans le fichier est enregistré à la position déterminée par {@link #SPACE_TAB_REFERENCE_POSITION} du même fichier.</li>
 * </ul>
 * <p>
 * De même, à l'ouverture du fichier, les deux fonctions suivantes sont appelées :
 * <ul>
 *     <li>{@link #readLinks()} qui permet de charger le tableau {@link #links} à partir du fichier, en chargeant un enregistrement dont la position est déterminée par {@link #LINKS_REFERENCE_POSITION}</li>
 *     <li>{@link #readFreeSpaceTab()} ()} qui permet de charger le tableau {@link #freeSpaceIntervals} à partir du fichier, en chargeant un enregistrement dont la position est déterminée par {@link #SPACE_TAB_REFERENCE_POSITION}</li>
 * </ul>
 *
 * @author Jason Mahdjoub
 * @version 1.0
 */
public class BDD implements AutoCloseable {
    private static final long LINKS_REFERENCE_POSITION = 0;
    private static final long SPACE_TAB_REFERENCE_POSITION = 8;

    static class FreeSpaceInterval implements Comparable<FreeSpaceInterval> {
        private long startPosition;
        private long length;

        FreeSpaceInterval(long startPosition, long length) {
            this.startPosition = startPosition;
            this.length = length;
        }

        long getStartPosition() {
            return startPosition;
        }

        long getLength() {
            return length;
        }

        void setStartPosition(long startPosition) {
            this.startPosition = startPosition;
        }

        void setLength(long length) {
            this.length = length;
        }

        @Override
        public int compareTo(FreeSpaceInterval o) {
            return Long.compare(startPosition, o.startPosition);
        }
    }


    private final RandomAccessFile raf;
    private TreeSet<FreeSpaceInterval> freeSpaceIntervals;
    private HashMap<String, Long> links;


    public BDD(File file) throws IOException, ClassNotFoundException {
        raf = new RandomAccessFile(file, "rw");

        System.out.println("length : " + raf.length());
        loadMetaData();
    }

    HashMap<String, Long> getLinks() {
        return links;
    }

    /**
     * Chargement des métadonnées
     *
     * @throws IOException            si un problème d'entrée/sortie se produit
     * @throws ClassNotFoundException si la déserialisation des données se passe mal.
     */
    private void loadMetaData() throws IOException, ClassNotFoundException {
        if (raf.length() == 0) {
            //le fichier est vide, les 16 premiers octets sont remplis par des références invalides qui seront complétés plus tard.
            raf.writeLong(-1);
            raf.writeLong(-1);
            //initialiation des métadonnées
            freeSpaceIntervals = new TreeSet<>();
            links = new HashMap<>();
        } else {
            //le fichier n'est pas vide, donc on charge les deux métadonnées de notre base de données.
            readLinks();
            readFreeSpaceTab();
        }
    }

    /**
     * Sauvegarde des métadonnées
     *
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private void saveMetaData() throws IOException {

        saveLinks();
        saveFreeSpaceTab();
    }


    /**
     * Ajout d'un enregistrement linéairement dans le fichier.
     * La fonction va serialiser l'élément en un tableau binaire en appelant la fonction {@link SerializationTools#serialize(Serializable)},
     * Puis elle va appeler la fonction {@link #putData(String, byte[])}
     *
     * @param objectName le nom de l'object à ajouter
     * @param object     l'objet/enregistrement à serializer, puis à ajouter.
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    public void putObject(String objectName, Serializable object) throws IOException {
        if (objectName != null) {
            byte[] array = new SerializationTools().serialize(object);
            putData(objectName, array);
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Ajout d'un enregistrement linéairement dans le fichier.
     * Avant l'ajout du nouvel élément, la fonction va supprimer l'élément associé à la clé donnée en paramètre en appelant la fonction {@link #removeObject(String)}, et ceci dans le cas où il existerait déjà dans la base de donnée.
     * Elle va ensuite rechercher une position où ajouter le fichier en appelant la fonction {@link #findPosition(byte[])}
     * Une fois l'ajout effectué, le tableau associatif {@link #links} est mis à jour, pour associer la clé de l'enregistrement avec la position de l'enregistrement dans le fichier.
     *
     * @param objectName la clé de l'object à ajouter
     * @param array      l'objet/enregistrement sous son format binaire à ajouter.
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private void putData(String objectName, byte[] array) throws IOException {
        removeObject(objectName);
        final long position = findPosition(array);
        this.links.put(objectName, position);
    }

    /**
     * Cette fonction ajoute la taille du tableau data à position donnée dans le fichier, suivi des données du tableau
     *
     * @param data le tableau binaire
     * @param pos  la position où placer les données dans le fichier {@link #raf}
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private void writeData(byte[] data, long pos) throws IOException {
        this.raf.seek(pos);
        this.raf.write(data);
    }

    /**
     * Cette fonction permet de récupérer l'enregistrement associé à la clé donnée en argument
     * Elle va rechercher la position de l'enregistrement dans le fichier grace au tableau associatif {@link #links},
     * Une fois l'enregistrement lu grace à la fonction {@link #readData(long), il est désérialisé grâce à la fonction {@link SerializationTools#deserialize(byte[])} pour être retourné.
     *
     * @param objectName la clé qui permet de retrouver l'objet enregistré
     * @return l'object enregistré correspondant ou null s'il n'est pas trouvé.
     * @throws IOException            si un problème d'entrée/sortie se produit
     * @throws ClassNotFoundException si l'object n'a pas pu être désérialisé
     */
    public Serializable getObject(String objectName) throws IOException, ClassNotFoundException {
        if (objectName != null) {
            long pos = this.links.get(objectName);
            byte[] tab = new byte[0];
            try {
                tab = readData(pos);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Serializable deserialized = SerializationTools.deserialize(tab);
            return deserialized;
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Cette fonction lit un tableau binaire dans le fichier {@link #raf} à la position donnée.
     * Pour connaître la quantité de données à enregistrer, la fonction commence par lire un entier dans le fichier ({@link RandomAccessFile#readInt()}).
     *
     * @param pos la position où commencer à lire dans le fichier {@link #raf}
     * @return l'enregistrement binaire lu
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private byte[] readData(long pos) throws Exception {
        this.raf.seek(pos);
        int size = this.raf.readInt();
        byte[] res = new byte[size];
        this.raf.read(res);
        return res;
    }

    /**
     * Cette fonction trouve une position libre dans le fichier {@link #raf} où enregistrer le tableau binaire donné en paramètre
     *
     * @param array le tableau binaire
     * @return la position trouvée
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private long findPosition(byte[] array) throws IOException {
        return findPosition(array.length + 4);
    }

    /**
     * Cette fonction trouve une position libre dans le fichier {@link #raf} où enregistrer des données binaires dont la taille est donnée en paramètre.
     * Pour se faire, elle appelle la fonction {@link #findPositionIntoFreeSpace(long)} pour vérifier qu'il n'y a pas une position qui pointe vers une zone contigüe libre au milieu du fichier et qui correspond au critère donné.
     * Si cette dernière fonction ne trouve pas de zone libre suffisement grande, la position retounée correspond à la fin du fichier ({@link RandomAccessFile#length()})
     *
     * @param desiredLength la taille de la zone libre souhaitée
     * @return la position trouvée
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private long findPosition(long desiredLength) throws IOException {
        Long position = findPositionIntoFreeSpace(desiredLength);
        if (position != null) {
            position = this.raf.length();
        } else {
            throw new NullPointerException();
        }
        return position;
    }

    /**
     * Cette fonction permet de chercher une position qui pointe vers une zone libre contigüe au milieu du fichier dont la taille est supérieure ou égale à la taille donnée en paramètre.
     * Cette fonction utilise le tableau {@link #freeSpaceIntervals} pour rechercher des zones libres.
     * Si une zone libre libre est trouvée, elle est amputée de l'espace à allouer.
     *
     * @param desiredLength la taille souhaitée en octet
     * @return la position trouvée, ou null si aucune position n'a été trouvée
     */
    private Long findPositionIntoFreeSpace(long desiredLength) {
        long position = 0;
        for (FreeSpaceInterval spaceInterval : this.freeSpaceIntervals) {
            if (spaceInterval.getLength() >= desiredLength) {
                position = spaceInterval.getStartPosition();
                return position;
            }
        }
        return null;
    }

    /**
     * Cette fonction supprime l'objet associé à la clé donnée en argument.
     * Elle récupère la position de l'objet dans le fichier. Si cette position existe, elle appelle la fonction {@link #removeObject(long)}.
     *
     * @param objectName la clé de l'objet
     * @return true si l'objet a été trouvé. False sinon.
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    public boolean removeObject(String objectName) throws IOException {
        if (objectName != null) {
            long position = 0;
            position = this.links.get(objectName);
            this.removeObject(position);
            return position != -1;
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Cette fonction supprime l'objet trouvé à la position donnée en argument.
     * Elle commence par lire la taille des données qui suivent, puis :
     * <ul>
     *     <li>Si l'objet est écrit jusqu'à la fin du fichier, ce dernier est tronqué grâce à la fonction {@link RandomAccessFile#setLength(long)}</li>
     *     <li>Sinon, un espace libre est ajouté dans le tableau {@link #freeSpaceIntervals}.
     *     	   Si cet espace libre est collé à un autre espace libre, à sa gauche et/ou à sa droite, ces derniers sont alors fusionnés
     *     </li>
     * </ul>
     *
     * @param pos la position où se trouve la donnée à supprimer
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private void removeObject(long pos) throws IOException {
        this.raf.seek(pos);
        int len = this.raf.readInt();
        if (pos + 4 + len == this.raf.length() - 1) {
            this.raf.setLength(pos);
            return;
        }
        FreeSpaceInterval precedent = null;
        FreeSpaceInterval remove = null;
        for (FreeSpaceInterval freeSpace : this.freeSpaceIntervals) {
            if (freeSpace.getStartPosition() + freeSpace.getLength() == pos - 1) {
                freeSpace.length += len;
                precedent = freeSpace;
            } else if (freeSpace.getStartPosition() == pos + len + 1) {
                if (precedent != null) {
                    precedent.length += freeSpace.getLength();
                    remove = freeSpace;
                } else {
                    this.freeSpaceIntervals.add(new FreeSpaceInterval(pos, len + freeSpace.getLength()));
                    remove = freeSpace;
                }
            }
        }
        if (remove != null) {
            this.freeSpaceIntervals.remove(remove);
        }
		/*
		try {
			int size = this.readData(pos).length;
			if( (this.raf.length()-pos) == size ){
				// Do nothing
			}else{
				for (FreeSpaceInterval spaceInterval:this.freeSpaceIntervals){
					//Si autre espace vide à droite, on fusionne
					if(spaceInterval.getStartPosition() == (pos+size)){
						size += spaceInterval.getLength();
					}
					//Si autre espace vide à gauche, on fusionne
					if(spaceInterval.getStartPosition() == (pos-size)){
						pos = spaceInterval.getStartPosition();
					}
				}
				this.freeSpaceIntervals.add(new FreeSpaceInterval(pos, size));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}*/
    }


    /**
     * Cette fonction sauvegarde le tableau associatif {@link #links} dans le fichier de la BDD :
     * <ol>
     *     <li>La fonction {@link #removeLinks()} est d'abord appelée pour supprimer l'ancienne version si elle existe</li>
     *     <li>{@link #links} est ensuite sérialisé grâce la fonction {@link SerializationTools#serialize(Serializable)}</li>
     *     <li>Une position pour stocker la nouvelle donnée est recherchée grâce à la fonction {@link #findPosition(byte[])}</li>
     *     <li>La fonction {@link #writeData(byte[], long)} permet ensuite d'enregistrer la donnée à la position déterminée précédement</li>
     *     <li>La position de la donnée est ensuite sauvegardée à la position {@link #LINKS_REFERENCE_POSITION}</li>
     * </ol>
     *
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private void saveLinks() throws IOException {
        removeLinks();
        if (this.links != null) {
            byte[] link_serialized = SerializationTools.serialize(this.links);
            long position = findPosition(link_serialized);
            writeData(link_serialized, position);
            writeData(SerializationTools.serialize(position), LINKS_REFERENCE_POSITION);
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Cette fonction lit la donnée à la position déterminée par {@link #LINKS_REFERENCE_POSITION}, grâce à la fonction {@link #readData(long)}.
     * Le tableau {@link #links} est ensuite déserialisé grâce à la fonction {@link SerializationTools#deserialize(byte[])}
     *
     * @throws IOException            si un problème d'entrée/sortie se produit
     * @throws ClassNotFoundException si la désérialisation se passe mal.
     */
    private void readLinks() throws IOException, ClassNotFoundException {
        raf.seek(LINKS_REFERENCE_POSITION);
        long pos = raf.readLong();
		byte [] tab = new byte[0];
		try {
			tab = readData(pos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.links = (HashMap<String, Long>) SerializationTools.deserialize(tab);

    }

    /**
     * Cette fonction supprime la donnée à la position déterminée par {@link #LINKS_REFERENCE_POSITION}, grâce à la fonction {@link #removeObject(long)}.
     * Si la position déterminée par {@link #LINKS_REFERENCE_POSITION} n'est pas supérieur à 16, aucune donnée n'est à supprimée.
     *
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private void removeLinks() throws IOException {
        if (LINKS_REFERENCE_POSITION > 16) {
            this.removeObject(LINKS_REFERENCE_POSITION);
        }

    }

    /**
     * Cette fonction sauvegade le tableau {@link #freeSpaceIntervals} dans le fichier de la BDD :
     * <ol>
     *     <li>La fonction {@link #removeFreeSpaceTab()} ()} est d'abord appelée pour supprimer l'ancienne version si elle existe</li>
     *     <li>{@link #freeSpaceIntervals} est ensuite sérialisé grâce la fonction {@link SerializationTools#deserializeFreeSpaceIntervals(byte[])}</li>
     *     <li>La position pour stocker la nouvelle donnée correspond à la fin du fichier de la BDD</li>
     *     <li>La fonction {@link #writeData(byte[], long)} permet ensuite d'enregistrer la donnée à la position déterminée précédement</li>
     *     <li>La position de la donnée est ensuite sauvegardée à la position {@link #SPACE_TAB_REFERENCE_POSITION}</li>
     *  </ol>
     *
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private void saveFreeSpaceTab() throws IOException {
        removeFreeSpaceTab();
        if (this.freeSpaceIntervals != null) {
            byte[] data = SerializationTools.serializeFreeSpaceIntervals(this.freeSpaceIntervals);
            long position = this.raf.length();
            writeData(data, position);
            writeData(SerializationTools.serialize(position), LINKS_REFERENCE_POSITION);
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Cette fonction lit la donnée à la position déterminée par {@link #SPACE_TAB_REFERENCE_POSITION}, grâce à la fonction {@link #readData(long)}.
     * Le tableau {@link #freeSpaceIntervals} est ensuite déserialisé grâce à la fonction {@link SerializationTools#deserializeFreeSpaceIntervals(byte[])}
     *
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private void readFreeSpaceTab() throws IOException {
        try {
            byte[] data = readData(SPACE_TAB_REFERENCE_POSITION);
            if (this.freeSpaceIntervals != null) {
                byte[] tab = SerializationTools.serializeFreeSpaceIntervals(this.freeSpaceIntervals);
            } else {
                throw new NullPointerException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Cette fonction supprime la donnée à la position déterminée par {@link #SPACE_TAB_REFERENCE_POSITION}, grâce à la fonction {@link #removeObject(long)}.
     * Si la position déterminée par {@link #SPACE_TAB_REFERENCE_POSITION} n'est pas supérieur à 16, aucune donnée n'est à supprimée.
     *
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    private void removeFreeSpaceTab() throws IOException {
        if (SPACE_TAB_REFERENCE_POSITION > 16) {
            removeObject(SPACE_TAB_REFERENCE_POSITION);
        }
    }

    @Override
    public void close() throws Exception {
        saveMetaData();
        raf.close();
    }


}
