package bdd;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
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
        if (o == null) {
            throw new NullPointerException();
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput output = new ObjectOutputStream(bos);
        output.writeObject(o);
        return bos.toByteArray();
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
        tab.close();
        obj.close();
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
                //dataOutputStream.flush();
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
        if (data != null) {
            TreeSet<BDD.FreeSpaceInterval> freeSpaceInterval = new TreeSet<BDD.FreeSpaceInterval>();

            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            byte[] first = new byte[8];
            byte[] second = new byte[8];

            ByteBuffer buffer1 = ByteBuffer.wrap(first);
            long posTab1 =  buffer1.getLong();

            ByteBuffer buffer2 = ByteBuffer.wrap(second);
            long posTab2 =  buffer2.getLong();

            while (bais.read(first) != -1 && bais.read(second) != -1) {
                freeSpaceInterval.add(new BDD.FreeSpaceInterval(posTab1, posTab1));
            }
            return freeSpaceInterval;
        } else {
            throw new NullPointerException();
        }
    }
}
