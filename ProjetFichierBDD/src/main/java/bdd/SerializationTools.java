package bdd;

import java.io.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Classe qui contient des outils de sérialization
 *
 * @author Jason Mahdjoub
 * @version 1.0
 */
class SerializationTools {
    /**
     * Serialise/binarise l'objet passé en paramètre pour retourner un tableau binaire
     *
     * @param o l'objet à serialiser
     * @return the tableau binaire
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    static byte[] serialize(Serializable o) throws IOException {
        if (o != null) {
            ByteArrayOutputStream tab = new ByteArrayOutputStream();
            ObjectOutputStream obj = new ObjectOutputStream(tab);
            obj.writeObject(o);
            obj.flush();
            obj.close();
            tab.close();
            return tab.toByteArray();
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Désérialise le tableau binaire donné en paramètre pour retrouver l'objet initial avant sa sérialisation
     *
     * @param data le tableau binaire
     * @return l'objet désérialisé
     * @throws IOException            si un problème d'entrée/sortie se produit
     * @throws ClassNotFoundException si un problème lors de la déserialisation s'est produit
     */
    static Serializable deserialize(byte[] data) throws IOException, ClassNotFoundException {

        ByteArrayInputStream tab = new ByteArrayInputStream(data);
        ObjectInputStream obj = new ObjectInputStream(tab);
        //tab.close();
        //obj.close();
        return (Serializable) obj.readObject();

    }

    /**
     * Serialise/binarise le tableau d'espaces libres passé en paramètre pour retourner un tableau binaire, mais selon le schéma suivant :
     * Pour chaque interval ;
     * <ul>
     *     <li>écrire en binaire la position de l'interval</li>
     *     <li>écrire en binaire la taille de l'interval</li>
     * </ul>
     * Utilisation pour cela la classe {@link DataOutputStream}
     *
     * @param freeSpaceIntervals le tableau d'espaces libres
     * @return un tableau binaire
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    static byte[] serializeFreeSpaceIntervals(TreeSet<BDD.FreeSpaceInterval> freeSpaceIntervals) throws IOException {
        if (freeSpaceIntervals != null) {
            ByteArrayOutputStream tab = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(tab);
            dataOutputStream.flush();
            tab.flush();
            for (BDD.FreeSpaceInterval interval : freeSpaceIntervals) {
                dataOutputStream.writeLong(interval.getStartPosition());
                dataOutputStream.writeLong(interval.getLength());
                dataOutputStream.flush();
            }
            dataOutputStream.close();
            tab.close();
            return tab.toByteArray();
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * Effectue l'opération inverse de la fonction {@link #serializeFreeSpaceIntervals(TreeSet)}
     *
     * @param data le tableau binaire
     * @return le tableau d'espaces libres
     * @throws IOException si un problème d'entrée/sortie se produit
     */
    static TreeSet<BDD.FreeSpaceInterval> deserializeFreeSpaceIntervals(byte[] data) throws IOException {
        if (data != null && data.length != 0) {
            TreeSet<BDD.FreeSpaceInterval> freeSpaceInterval = new TreeSet<BDD.FreeSpaceInterval>();
            for (int i = 0; i < data.length; i = i + 16) {
                try {
                    ByteArrayInputStream tab = new ByteArrayInputStream(Arrays.copyOfRange(data, i, i + 8));
                    ObjectInputStream obj = new ObjectInputStream(tab);
                    freeSpaceInterval.add((BDD.FreeSpaceInterval) obj.readObject());
                    tab.close();
                    obj.close();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return null;
        } else {
            throw new NullPointerException();
        }
    }
}
