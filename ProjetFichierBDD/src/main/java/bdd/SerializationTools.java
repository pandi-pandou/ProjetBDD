package bdd;

import java.io.*;
import java.util.TreeSet;

/**
 * Classe qui contient des outils de sérialization
 *
 * @author Jason Mahdjoub
 * @version 1.0
 */
class SerializationTools implements Serializable{
	/**
	 * Serialise/binarise l'objet passé en paramètre pour retourner un tableau binaire
	 * @param o l'objet à serialiser
	 * @return the tableau binaire
	 * @throws IOException si un problème d'entrée/sortie se produit
	 */
	static byte[] serialize(Serializable o) throws IOException {
		if(o != null) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			byte[] binaryTab = null;
			try {
				oos.writeObject(o);
				oos.flush();
				binaryTab = bos.toByteArray();
			} finally {
				try {
					if (bos != null) {
						bos.close();
					}
				} catch (IOException e) {
					throw new IOException(e);
				}
			}
			return binaryTab;
		} else{
			throw new NullPointerException("Object is null.");
		}

	}

	/**
	 * Désérialise le tableau binaire donné en paramètre pour retrouver l'objet initial avant sa sérialisation
	 * @param data le tableau binaire
	 * @return l'objet désérialisé
	 * @throws IOException si un problème d'entrée/sortie se produit
	 * @throws ClassNotFoundException si un problème lors de la déserialisation s'est produit
	 */
	static Serializable deserialize(byte[] data) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInput in = null;
		Serializable o = null;
		try {
			in = new ObjectInputStream(bis);
			o = (Serializable) in.readObject();
		}catch (final ClassNotFoundException e) {
			e.printStackTrace();
		} finally
		 {
			try {
				if (in != null) {
					in.close();
				}
			} catch (IOException e) {
				throw new IOException(e);
			}
		}
		return o;
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
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = null;
		byte[] result = null;
		try {
			dos = new DataOutputStream(baos);

			for(BDD.FreeSpaceInterval interval : freeSpaceIntervals){
				dos.writeLong(interval.getStartPosition());
				dos.writeLong(interval.getLength());
				dos.flush();
				result = baos.toByteArray();
			}
		} catch (IOException e) {
			throw new IOException(e);
		}
		finally {
			try {
				if(baos!=null)
					baos.close();
			} catch (IOException e) {
				throw new IOException(e);
			}
			try {
				if(dos!=null)
					dos.close();
			} catch (IOException e) {
				throw new IOException(e);
			}
		}

		return result;
	}

	/**
	 * Effectue l'opération inverse de la fonction {@link #serializeFreeSpaceIntervals(TreeSet)}
	 * @param data le tableau binaire
	 * @return le tableau d'espaces libres
	 * @throws IOException si un problème d'entrée/sortie se produit
	 */
	static TreeSet<BDD.FreeSpaceInterval> deserializeFreeSpaceIntervals(byte[] data) throws IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		DataInputStream dis = null;
		Serializable o = null;
		try {
			dis = new DataInputStream(bis);
			o = (Serializable) dis.read(data);
		}
		finally
		{
			try {
				if (dis != null) {
					dis.close();
				}
			} catch (IOException e) {
				throw new IOException(e);
			}
		}
		return (TreeSet<BDD.FreeSpaceInterval>) o;
	}
}
