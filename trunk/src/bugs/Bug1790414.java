package bugs;

import java.nio.charset.Charset;
import java.util.*;

public class Bug1790414 {

	public static void main(String[] argv) throws Exception {
		Collection charsets = Charset.availableCharsets().values();
		Iterator iterator = charsets.iterator();
		while (iterator.hasNext()) {
			Charset charset = (Charset)iterator.next();
			System.out.print(charset.name());
			System.out.print('\t');
			System.out.print(charset.newDecoder().averageCharsPerByte());
			System.out.println();
		}
	}

} // class Bug1790414