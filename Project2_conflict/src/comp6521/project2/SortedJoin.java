package comp6521.project2;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import comp6521.project2.utils.ByteArrayComparator;
import comp6521.project2.utils.Constants;
import comp6521.project2.utils.Utils;

public class SortedJoin {

	ByteArrayComparator bac = new ByteArrayComparator();

	public void sortJoin(String inputFile1, String inputFile2, String outputFile,final String gradesFile) throws IOException {

System.out.println(Constants.TUPPLES_IN_BUFFER_T1_SORTED_JOIN+" "+Constants.TUPPLES_IN_BUFFER_T2_SORTED_JOIN+" "+
		Constants.TUPPLE_FOR_SORTED_JOINED_OUTPUT+" "+Constants.TUPPLE_FOR_GARDES_OUTPUT);
		int noOfRecordsInFile1 = Utils.findRecordsInFile(inputFile1,
				(Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.LINE_SEPARATOR_LENGTH));
		

		try (ReadableByteChannel inChannel1 = Channels.newChannel(new FileInputStream(inputFile1));
				ReadableByteChannel inChannel2 = Channels.newChannel(new FileInputStream(inputFile2));
				WritableByteChannel outChannel = Channels.newChannel(new FileOutputStream(outputFile));
				WritableByteChannel gradesChannel = Channels.newChannel(new FileOutputStream(gradesFile))) {

			ByteBuffer buffer1 = ByteBuffer.allocateDirect(Constants.TUPPLES_IN_BUFFER_T1_SORTED_JOIN
					* (Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.LINE_SEPARATOR_LENGTH));
			ByteBuffer buffer2 = ByteBuffer.allocateDirect(Constants.TUPPLES_IN_BUFFER_T2_SORTED_JOIN
					* (Constants.TUPLE_SIZE_IN_BYTES_T2 + Constants.LINE_SEPARATOR_LENGTH));

			ByteBuffer outputBuffer = ByteBuffer.allocateDirect(Constants.TUPPLE_FOR_SORTED_JOINED_OUTPUT
					* (Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.TUPLE_SIZE_IN_BYTES_T2 - Constants.STUDENT_ID_LENGTH
							+ Constants.LINE_SEPARATOR_LENGTH));
			ByteBuffer gradesBuffer = ByteBuffer.allocateDirect(Constants.TUPPLE_FOR_GARDES_OUTPUT
					* (Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.TUPLE_SIZE_IN_BYTES_T2 - Constants.STUDENT_ID_LENGTH
							+ Constants.LINE_SEPARATOR_LENGTH));

			byte[] record1;
			byte[] record2;

			int startPointer1 = 0;

			inChannel2.read(buffer2);
			buffer2.flip();

			record1 = new byte[Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.LINE_SEPARATOR_LENGTH];
			record2 = new byte[Constants.TUPLE_SIZE_IN_BYTES_T2 + Constants.LINE_SEPARATOR_LENGTH];
			
			int studentId_OLD=0;
			int studentId_NEW;
			int denominator=0;
			float numerator = 0;
			buffer2.get(record2);
			
			while (startPointer1 < noOfRecordsInFile1) {
				
				buffer1.clear();
				inChannel1.read(buffer1);
				
				startPointer1 += (buffer1.position()
						/ (Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.LINE_SEPARATOR_LENGTH));
				
				buffer1.flip();
				studentId_OLD = Utils.getIntegerData(record1,0,8);
				
				
				while (buffer1.hasRemaining() || bac.compare(record1, record2)==0) {					
					int value = bac.compare(record1, record2);
					
					if (value == 0) // ids are equal
					{
						studentId_NEW = Utils.getIntegerData(record2,0,8);
						if(studentId_OLD==studentId_NEW) {
							int credit = Utils.getIntegerData(record2, 21, 2);
							String grade = Utils.getStringData(record2, 23, 4);
							//System.out.println(new String(grade));
							numerator += credit*Utils.gradeToMarks(grade);
							denominator+= credit;							
						}
						
						if (outputBuffer.position() < outputBuffer.capacity()) {
							outputBuffer.put(Utils.combine(record1, record2));
						} else {
							outputBuffer.flip();
							outChannel.write(outputBuffer);
							outputBuffer.clear();
							outputBuffer.put(Utils.combine(record1, record2));			
						}
						
						if (buffer2.hasRemaining()) {
							buffer2.get(record2);
						} 
						else {								// id in t1 is bigger
							buffer2.clear();
							inChannel2.read(buffer2);
							buffer2.flip();
						}
					
					} else if (value > 0) // id in t2 is bigger
					{
						if (buffer1.hasRemaining()) {
							buffer1.get(record1); // id in t1 is bigger
							if(denominator>0) {
								if(gradesBuffer.position()==gradesBuffer.capacity())
								{
									gradesBuffer.flip();
									gradesChannel.write(gradesBuffer);
									gradesBuffer.clear();
								}
								gradesBuffer.put(Utils.convertToBuffer(studentId_OLD,String.format("%.2f", numerator/denominator).getBytes()));
							}
							studentId_OLD = Utils.getIntegerData(record1,0,8);
							numerator=0;
							denominator=0;
						}
						else {
							buffer1.clear();
							inChannel1.read(buffer1);
							startPointer1 += (buffer1.position()
									/ (Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.LINE_SEPARATOR_LENGTH));
							buffer1.flip();

						}
					}
					else {						// id in t1 is bigger
						if (buffer2.hasRemaining())
							buffer2.get(record2); 
						else {
							buffer2.clear();
							inChannel2.read(buffer2);
							buffer2.flip();
						}
					}
				}
				System.out.println(startPointer1);
			}
			if (outputBuffer.position() > 0) {
				outputBuffer.flip();
				outChannel.write(outputBuffer);
				outputBuffer = null;
			}
			if(denominator>0) {
				gradesBuffer.put(Utils.convertToBuffer(studentId_OLD,String.format("%.2f", numerator/denominator).getBytes()));
			}
			if (gradesBuffer.position() > 0) {
				gradesBuffer.flip();
				gradesChannel.write(gradesBuffer);
				gradesBuffer = null;
			}
			
		}
	}

}
