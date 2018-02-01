package utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Storage object class Contains static methods for
 * serialization/deserialization and clone of objects
 * 
 * @author ksemer
 *
 */
public class Storage {

	// =================================================================
	private static final long MEGABYTE = 1024L * 1024L;
	// =================================================================

	/**
	 * Serialize the given object to given file name
	 * 
	 * @param object
	 * @param fileOutputPath
	 */
	public static void serialize(Object object, String fileName) {
		long executionTime = System.currentTimeMillis();

		try {
			FileOutputStream fileOut = new FileOutputStream(fileName);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(object);
			out.close();
			fileOut.close();
			System.out.println("Serialized data is saved in " + fileName);
		} catch (IOException i) {
			i.printStackTrace();
		}
		System.out.println("Serialized time: " + (System.currentTimeMillis() - executionTime) / 1000 + " (sec)");
	}

	/**
	 * Deserialize the given file
	 * 
	 * @param fileName
	 * @return
	 */
	public static Object deserialize(String fileName) {
		Object object = null;
		long executionTime = System.currentTimeMillis();

		try {
			System.out.println("Deserializing file " + fileName);
			FileInputStream inputFileStream = new FileInputStream(fileName);
			ObjectInputStream objectInputStream = new ObjectInputStream(inputFileStream);
			object = objectInputStream.readObject();
			objectInputStream.close();
			inputFileStream.close();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException i) {
			i.printStackTrace();
		}

		System.out.println("Deserialized time: " + (System.currentTimeMillis() - executionTime) / 1000 + " (sec)");

		return object;
	}

	/**
	 * This method makes a "deep clone" of the given object
	 */
	public static Object deepClone(Object object) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(object);
			oos.flush();
			oos.close();
			baos.close();
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			return ois.readObject();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Convert bytes to megabytes
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesToMegabytes(long bytes) {
		return (bytes / MEGABYTE) + " (mb)";
	}
}