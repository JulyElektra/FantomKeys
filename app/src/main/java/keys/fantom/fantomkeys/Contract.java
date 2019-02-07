package keys.fantom.fantomkeys;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;

import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.Sign;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.testing.http.MockHttpContent;
import com.google.common.collect.Lists;

public class Contract {

	private String callerAddress;
	private String contractAddress;

	private String fantomUrl;

	public String getFantomUrl() {
		return fantomUrl;
	}

	public void setFantomUrl(String fantomUrl) {
		this.fantomUrl = fantomUrl;
	}

	public String getCallerAddress() {
		return callerAddress;
	}

	public String getContractAddress() {
		return contractAddress;
	}

	public void setCallerAddress(String callerAddress) {
		this.callerAddress = callerAddress;
	}

	public void setContractAddress(String contractAddress) {
		this.contractAddress = contractAddress;
	}

    public Contract(String callerAddress, String contractAddress, String fantomUrl) {
        this.callerAddress = callerAddress;
        this.contractAddress = contractAddress;
        this.fantomUrl = fantomUrl;
    }

    public boolean isBooked(long id, long from, long to) throws IOException {


//		Function function = new Function("isBooked",
//                Arrays.asList(new Uint256(id), new Uint256(from), new Uint256(to)),
//                Arrays.asList(new TypeReference<Bool>() {})
//            );

        List<Type> parameters = Lists.<Type>newArrayList(new Uint256(id), new Uint256(from), new Uint256(to));
        List<TypeReference<?> >returnType = Lists.<TypeReference<?>>newArrayList(new TypeReference<Bool>() {});
        Function function = new Function("isBooked", parameters, returnType);

            String encodedFunction = FunctionEncoder.encode(function);

            JSONObject object = new JSONObject();
            object.put("from", callerAddress);
            object.put("value", 0);
            object.put("to", contractAddress);
            object.put("data", encodedFunction);
            object.put("nonce", 0);
            object.put("gas", 1000000);
            object.put("gasPrice", 0);

            HttpRequestFactory requestFactory = new NetHttpTransport()          .createRequestFactory();
            MockHttpContent mock = new MockHttpContent();
            mock.setType("application/json");
            mock.setContent(object.toJSONString().getBytes("UTF-8"));

            HttpRequest req = requestFactory.buildPostRequest(new GenericUrl(fantomUrl + "/call"), mock);
            HttpResponse rs = req.execute();
            String rss = inputStreamToString(rs.getContent());
            return rss.contains("000000001");
	}

	public String book(long id, long from, long to, BigInteger priv) throws IOException, ParseException {

		Function function = new Function("book",
                Arrays.<Type>asList(
                		new Uint256(id),
                		new Uint256(from),
                		new Uint256(to),
                		new Address(callerAddress)),
                Arrays.<TypeReference<?>>asList()
            );
            String encodedFunction = FunctionEncoder.encode(function);

            RawTransaction rawTx = RawTransaction.createTransaction(
            		new BigInteger(String.valueOf(getNonce())),
            		new BigInteger("1"),
            		new BigInteger("2560000"),
            		contractAddress,
            		new BigInteger("0"),
            		encodedFunction);

            BigInteger pub = Sign.publicKeyFromPrivate(priv);
    		ECKeyPair key = new ECKeyPair(priv, pub);

    		Credentials credentials = Credentials.create(key);
            byte[] tx = TransactionEncoder.signMessage(rawTx, credentials);

            String txHex = Numeric.toHexString(tx);

            HttpRequestFactory requestFactory = new NetHttpTransport()          .createRequestFactory();
            MockHttpContent mock = new MockHttpContent();
            mock.setContent(txHex.getBytes("UTF-8"));

            HttpRequest req = requestFactory.buildPostRequest(new GenericUrl(fantomUrl + "/sendRawTransaction"), mock);
            HttpResponse rs = req.execute();
            String rss = inputStreamToString(rs.getContent());

            JSONParser parser = new JSONParser();
            JSONObject jo = (JSONObject) parser.parse(rss);

            String txHash = (String) jo.get("txHash");
            System.out.println("txHash: " + txHash);
            return txHash;
	}

	private int getNonce() throws IOException, ParseException {
		HttpRequestFactory requestFactory = new NetHttpTransport()          .createRequestFactory();
        HttpRequest req = requestFactory.buildGetRequest(new GenericUrl(fantomUrl + "/account/" + callerAddress));
        HttpResponse rs = req.execute();
        String rss = inputStreamToString(rs.getContent());

        Matcher m = java.util.regex.Pattern.compile(":([0-9]+)\\}").matcher(rss);
        m.find();
        int nonce = Integer.parseInt(m.group(1));
        System.out.println(nonce);
        return nonce;
	}

	public static String inputStreamToString(InputStream inputStream) throws IOException {
        try(ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            return result.toString("UTF-8");
        }
    }
}
