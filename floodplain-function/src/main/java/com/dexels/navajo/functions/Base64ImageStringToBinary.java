/**
 * <p>Title: Navajo Product Project</p>
 * <p>Description: This is the official source for the Navajo server</p>
 * <p>Copyright: Copyright (c) 2005</p>
 * <p>Company: Dexels BV</p>
 * @author 
 * @version $Id$.
 *
 * DISCLAIMER
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL DEXELS BV OR ITS CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */
package com.dexels.navajo.functions;

import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dexels.navajo.document.types.Binary;
import com.dexels.navajo.expression.api.FunctionInterface;
import com.dexels.navajo.expression.api.TMLExpressionException;

import java.nio.charset.StandardCharsets;

public class Base64ImageStringToBinary extends FunctionInterface {

	private final static Logger logger = LoggerFactory.getLogger(Base64ImageStringToBinary.class);
	
	@Override
	public String remarks() {
		return "Get a binary from a Base64 representation of a given string.";
	}

	@Override
	public String usage() {
		return "Base64ImageStringToBinary(String)";
	}
	
	@Override
	public boolean isPure() {
		return true;
	}

    
    // I think this function does not work when you pass a Binary into it.
    // It is a bit pointless anyway, it is more like a 'clone' then.

	@Override
	public Object evaluate() throws TMLExpressionException {
		String data = getStringOperand(0);
		Binary b;
		try {
			String partSeparator = ",";
			String dataSeperator = ":";
			
			if (data.contains(partSeparator)) {
				String encodedImg = data.split(partSeparator)[1];						
				String mimeType = data.split(dataSeperator)[1].split(";")[0];
						
				byte[] decodedValue = Base64.getDecoder().decode(encodedImg.getBytes(StandardCharsets.UTF_8));  // Basic Base64 decoding
				String parsedData = new String(decodedValue, StandardCharsets.UTF_8);
				
				final StringReader reader = new StringReader(parsedData);
				b = new Binary(reader);
				b.setMimeType(mimeType);
				return b;
			}	
			return null;
		} catch (IOException e) {
			logger.error("Error: ", e);
			return null;
		}
		
	}

}