/**
 * 
 */
package comp6521.project2;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import comp6521.project2.utils.Constants;
import comp6521.project2.utils.Utils;

/**
 * @author AmanRana
 *
 */
public class Aman {

	public void join(final String inputFile1, final String inputFile2, final String outputFile) throws IOException {
		try (ReadableByteChannel inChannel1 = Channels.newChannel(new FileInputStream(inputFile1));
				ReadableByteChannel inChannel2 = Channels.newChannel(new FileInputStream(inputFile2));
				WritableByteChannel outChannel = Channels.newChannel(new FileOutputStream(outputFile));)

		{
			ByteBuffer buffer1 = ByteBuffer.allocateDirect(Constants.TUPPLES_IN_BUFFER_T1_NESTED_JOIN
					* (Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.LINE_SEPARATOR_LENGTH));

			ByteBuffer buffer2 = ByteBuffer.allocateDirect(Constants.TUPPLES_IN_BUFFER_T2_NESTED_JOIN
					* (Constants.TUPLE_SIZE_IN_BYTES_T2 + Constants.LINE_SEPARATOR_LENGTH));

			ByteBuffer outBuffer = ByteBuffer.allocateDirect(Constants.TUPPLE_FOR_JOINED_OUTPUT
					* (Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.TUPLE_SIZE_IN_BYTES_T2 - Constants.STUDENT_ID_LENGTH
							+ Constants.LINE_SEPARATOR_LENGTH));

			byte[] receive1 = new byte[Constants.TUPLE_SIZE_IN_BYTES_T1 + Constants.LINE_SEPARATOR_LENGTH];
			byte[] receive2 = new byte[Constants.TUPLE_SIZE_IN_BYTES_T2 + Constants.LINE_SEPARATOR_LENGTH];
			while (inChannel1.read(buffer1) > 0) {
				buffer1.flip();
				while (buffer1.hasRemaining()) {
					buffer1.get(receive1);

					while (inChannel2.read(buffer2) > 0) {
						buffer2.flip();

						while (buffer2.hasRemaining()) {

							buffer2.get(receive2);
							if (Utils.compare(receive1, receive2)) {
								if (outBuffer.position() == outBuffer.capacity()) {
									outBuffer.flip();
									outChannel.write(outBuffer);
									outBuffer.clear();
								}
								outBuffer.put(Utils.combine(receive1, receive2));
							}
						}
					}

				}
			}

		}
	}

}
