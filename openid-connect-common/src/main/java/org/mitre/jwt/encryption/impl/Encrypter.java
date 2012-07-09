package org.mitre.jwt.encryption.impl;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.mitre.jwe.model.Jwe;
import org.mitre.jwe.model.JweHeader;
import org.mitre.jwt.encryption.AbstractJweEncrypter;
import org.mitre.jwt.model.JwtClaims;
import org.mitre.jwt.signer.impl.HmacSigner;

public class Encrypter extends AbstractJweEncrypter {
	
	private Jwe jwe;
	
	private JweHeader header;
	
	private JwtClaims claims;
	
	private String signature;
	
	public Encrypter(Jwe jwe) {
		setJwe(jwe);
		setHeader(jwe.getHeader());
		setClaims(jwe.getClaims());
		setSignature(jwe.getSignature());
	}
	
	public Jwe getJwe() {
		return jwe;
	}

	public void setJwe(Jwe jwe) {
		this.jwe = jwe;
	}
	
	
	public JweHeader getHeader() {
		return header;
	}

	public void setHeader(JweHeader header) {
		this.header = header;
	}

	public JwtClaims getClaims() {
		return claims;
	}

	public void setClaims(JwtClaims claims) {
		this.claims = claims;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	@Override
	public Jwe encryptAndSign(Jwe jwe) throws NoSuchAlgorithmException {
		
		String alg = jwe.getHeader().getAlgorithm();
		String iv = jwe.getHeader().getIntegrity();
		
		if(alg.equals("RS256") || alg.equals("RS384") || alg.equals("RS512")) {
			
			//generate CEK and CIK
			
			PrivateKey contentEncryptionKey = null;
			PublicKey contentIntegrityKey = null;
			
			try {
				
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance(jwe.getHeader().getKeyDerivationFunction());
				KeyPair keyPair = keyGen.genKeyPair();
				contentEncryptionKey = keyPair.getPrivate();
				contentIntegrityKey = keyPair.getPublic();
				
			} catch (NoSuchAlgorithmException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			jwe.setCiphertext(encryptClaims(jwe, contentEncryptionKey));
			jwe.setEncryptedKey(encryptKey(jwe));
			
			if(iv.equals("HS256") || iv.equals("HS384") || iv.equals("HS512")){
			
				HmacSigner hmacSigner = new HmacSigner(contentIntegrityKey.getEncoded()); 
				jwe = (Jwe) hmacSigner.sign(jwe);
				
			} else if(iv.equals("RS256") || iv.equals("RS384") || iv.equals("RS512")) {
				throw new IllegalArgumentException("Integrity Value must use Hmac signing");
			} else {
				throw new IllegalArgumentException("Not a valid integrity value algorithm");
			}
			
		} else if(alg.equals("HS256") || alg.equals("HS384") || alg.equals("HS512")){
			throw new IllegalArgumentException("Cannot use Hmac for encryption");
		} else {
			throw new IllegalArgumentException("Not a valid signing algorithm");
		}
		
		return jwe;
	}

}
