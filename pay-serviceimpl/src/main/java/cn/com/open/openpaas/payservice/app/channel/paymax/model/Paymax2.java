package cn.com.open.openpaas.payservice.app.channel.paymax.model;

import cn.com.open.openpaas.payservice.app.channel.paymax.config.PaymaxConfig;
import cn.com.open.openpaas.payservice.app.channel.paymax.config.SignConfig;
import cn.com.open.openpaas.payservice.app.channel.paymax.exception.AuthorizationException;
import cn.com.open.openpaas.payservice.app.channel.paymax.exception.InvalidRequestException;
import cn.com.open.openpaas.payservice.app.channel.paymax.exception.InvalidResponseException;
import cn.com.open.openpaas.payservice.app.channel.paymax.sign.HttpRequestWrapper;
import cn.com.open.openpaas.payservice.app.channel.paymax.sign.RSA;
import cn.com.open.openpaas.payservice.app.channel.paymax.sign.Request;

import com.alibaba.fastjson.JSON;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Created by xiaowei.wang on 2016/4/26.
 */
public abstract class Paymax2 extends PaymaxBase {

    private static String HEADER_KEY_NONCE = "nonce";
    private static String HEADER_KEY_TIMESTAMP = "timestamp";
    private static String HEADER_KEY_AUTHORIZATION= "Authorization";
    private static String REQUEST_SUCCESS_FLAG = "reqSuccessFlag";
    private static String RESPONSE_CODE = "code";
    private static String RESPONSE_DATA = "data";
    private static int VALID_RESPONSE_TTL=2*60*1000;//合法响应时间:2分钟内


    private static CloseableHttpClient httpsClient = null;

    static class AnyTrustStrategy implements TrustStrategy {

        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            return true;
        }

    }

    static {
        try {
            RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.create();
            ConnectionSocketFactory plainSF = new PlainConnectionSocketFactory();
            registryBuilder.register("http", plainSF);

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            SSLContext sslContext =
                    SSLContexts.custom().useTLS().loadTrustMaterial(trustStore, new AnyTrustStrategy()).build();
            LayeredConnectionSocketFactory sslSF =
                    new SSLConnectionSocketFactory(sslContext, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            registryBuilder.register("https", sslSF);

            Registry<ConnectionSocketFactory> registry = registryBuilder.build();

            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);
            connManager.setMaxTotal(500);
            httpsClient = HttpClientBuilder.create().setConnectionManager(connManager).build();
        
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param url
     * @param jsonReqData
     * @param clazz
     * @param <T>
     * @return
     * @throws AuthorizationException
     * @throws IOException
     * @throws InvalidRequestException
     */
    protected static <T> T request(String url, String jsonReqData, Class<T> clazz,String private_key,String secret_key,String paymax_public_key) throws AuthorizationException, IOException, InvalidRequestException, InvalidResponseException {
        if(StringUtils.isBlank(SignConfig.SECRET_KEY)){
            throw new AuthorizationException("Secret key can not be blank.Please set your Secret key in com.Paymax.config.SignConfig");
        }

        if(StringUtils.isBlank(SignConfig.PRIVATE_KEY)){
            throw new AuthorizationException("RSA Private key can not be blank.Please set your RSA Private key  in com.Paymax.config.SignConfig");
        }

        if(StringUtils.isBlank(SignConfig.PAYMAX_PUBLIC_KEY)){
            throw new InvalidRequestException("Paymax Public key can not be blank.Please set your Paymax Public key in com.Paymax.config.SignConfig");
        }
        Map<String, String> result = null;
        if (StringUtils.isBlank(jsonReqData)){
            result = buildGetRequest(url,private_key,secret_key,paymax_public_key);
        }else {
        	//System.out.println(jsonReqData);
            result = buildPostRequest(url, jsonReqData,secret_key,private_key,paymax_public_key);
        }

        return dealWithResult(result,clazz);

    }

    /**
     * 处理返回数据
     *
     * @param result
     * @param clazz
     * @param <T>
     * @return
     */
    private static <T> T dealWithResult(Map<String, String> result,Class<T> clazz) {
        int resultCode = Integer.valueOf(result.get(RESPONSE_CODE)).intValue();
        String resultData = result.get(RESPONSE_DATA);

        T t = JSON.parseObject(resultData,clazz);
        try {
            if (t==null){
                t = clazz.newInstance();
            }

            Field f = clazz.getDeclaredField(REQUEST_SUCCESS_FLAG);
            f.setAccessible(true);
            f.set(t,resultCode<400);

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return t;

    }

    /**
     * POST请求
     * @param url
     * @param jsonReqData
     * @return
     * @throws IOException
     * @throws InvalidResponseException
     */
    private static  Map<String, String> buildPostRequest(String url, String jsonReqData,String secret_key,String private_key,String paymax_public_key) throws IOException, InvalidResponseException {
        Map<String, String> result = null;

        HttpPost httpPost = new HttpPost(url);

        httpPost.setEntity(
                new ByteArrayEntity(jsonReqData.getBytes(Charset.forName(PaymaxConfig.CHARSET)), ContentType.APPLICATION_JSON));

        setCustomHeaders(httpPost,secret_key);

        String sign = signData(httpPost,private_key);

        httpPost.addHeader(PaymaxConfig.SIGN,sign);
        CloseableHttpResponse response =null;
        try {
        	response= httpsClient.execute(httpPost);
       
         result = verifyData(response,paymax_public_key);
        } finally {
            response.close();
            httpPost.releaseConnection();
            httpsClient.getConnectionManager().shutdown();
        }

        return result;

    }

    /**
     * 对返回结果进行验签。
     *  验签成功:返回数据
     *  验签失败:抛出异常
     *
     * @param response
     * @return
     * @throws IOException
     * @throws InvalidResponseException
     */
    private static Map<String, String>  verifyData(CloseableHttpResponse response,String paymax_public_key ) throws IOException, InvalidResponseException {
        Map<String, String> result = null;
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            try {

                String resData = EntityUtils.toString(entity, Charset.forName(PaymaxConfig.CHARSET));
                //resData=URLEncoder.encode(resData);
              //  System.out.println(resData);
                int responseCode = response.getStatusLine().getStatusCode();
                if (Integer.valueOf(responseCode)<400){
                    String nonce = response.getFirstHeader(HEADER_KEY_NONCE)!=null ? response.getFirstHeader(HEADER_KEY_NONCE).getValue() : "";
                    String timestamp = response.getFirstHeader(HEADER_KEY_TIMESTAMP)!=null ? response.getFirstHeader(HEADER_KEY_TIMESTAMP).getValue() : "";
                    String secretKey = response.getFirstHeader(HEADER_KEY_AUTHORIZATION)!=null ? response.getFirstHeader(HEADER_KEY_AUTHORIZATION).getValue() : "";

                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    out.write(nonce.getBytes(PaymaxConfig.CHARSET));
                    out.write('\n');//header
                    out.write(timestamp.getBytes(PaymaxConfig.CHARSET));
                    out.write('\n');//header
                    out.write(secretKey.getBytes(PaymaxConfig.CHARSET));
                    out.write('\n');//header
                  /*  byte[] data = resData.getBytes(PaymaxConfig.CHARSET);
                    out.write(data);
                    String aa=new String( data);*/
                    String toVerifyData = new String( out.toByteArray())+resData;
                    //toVerifyData=URLDecoder.decode(toVerifyData);
                   // System.out.println(toVerifyData);
                    String sign = response.getFirstHeader(PaymaxConfig.SIGN)!=null ? response.getFirstHeader(PaymaxConfig.SIGN).getValue() : "";
                    boolean flag = RSA.verify(toVerifyData,sign,paymax_public_key);
                    if (!flag){
                        throw new InvalidResponseException("Invalid Response.[Response Data And Sign Verify Failure.]");
                    }

//                    if (!SignConfig.SECRET_KEY.equals(secretKey)){
//                        throw new InvalidResponseException("Invalid Response.[Secret Key Is Invalid.]");
//                    }

                    if (Long.valueOf(timestamp) + VALID_RESPONSE_TTL < System.currentTimeMillis()){
                        throw new InvalidResponseException("Invalid Response.[Response Time Is Invalid.]");
                    }
                }


                result = new HashMap<String, String>();

                result.put(RESPONSE_CODE, String.valueOf(responseCode));
                result.put(RESPONSE_DATA, resData);

            } finally {
                EntityUtils.consumeQuietly(entity);
            }

        }

        return result;
    }


    /**
     * GET请求
     * @param url
     * @return
     * @throws IOException
     * @throws InvalidResponseException
     */
    public static Map<String, String> buildGetRequest(String url,String private_key,String secret_key,String paymax_public_key) throws IOException, InvalidResponseException {
        Map<String, String> result = null;

        HttpGet httpGet = new HttpGet(url);

        setCustomHeaders(httpGet,secret_key);

        String sign = signData(httpGet,private_key);

        httpGet.addHeader(PaymaxConfig.SIGN,sign);

        CloseableHttpResponse response = httpsClient.execute(httpGet);
        try {
            result = verifyData(response,paymax_public_key);
        } finally {
            response.close();
            httpGet.releaseConnection();
            httpsClient.getConnectionManager().shutdown();
        }

        return result;
    }

    /**
     * 设置header
     *
     * Request Header
     * @param request
     */
    private static void setCustomHeaders(HttpRequestBase request,String secret_key) {
        request.addHeader("Content-Type", "application/json; charset=" + PaymaxConfig.CHARSET);
        request.addHeader(HEADER_KEY_AUTHORIZATION,secret_key);
        //request.addHeader(HEADER_KEY_AUTHORIZATION,SignConfig.SECRET_KEY);

        String timestamp = String.valueOf(System.currentTimeMillis());
        request.addHeader(HEADER_KEY_TIMESTAMP, timestamp);
        request.addHeader(HEADER_KEY_NONCE,UUID.randomUUID().toString().replaceAll("-",""));

        String[] propertyNames = {"os.name", "os.version", "os.arch",
                "java.version", "java.vendor", "java.vm.version",
                "java.vm.vendor"};
        Map<String, String> propertyMap = new HashMap<String, String>();
        for (String propertyName : propertyNames) {
            propertyMap.put(propertyName, System.getProperty(propertyName));
        }
        propertyMap.put("lang", "Java");
        propertyMap.put("publisher", "Paymax");
        propertyMap.put("sdk-version",PaymaxConfig.SDK_VERSION);

        request.addHeader("X-Paymax-Client-User-Agent", JSON.toJSONString(propertyMap));

    }

    /**
     * 签名数据
     *
     * @param httpRequest
     * @return
     * @throws IOException
     */
    private static String signData(HttpRequestBase httpRequest,String private_key) throws IOException {
        Request<HttpRequest> request = new HttpRequestWrapper(
                httpRequest);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(request.getMethod().toLowerCase().getBytes(PaymaxConfig.CHARSET));
        out.write('\n');//method
        String uri=request.getRequestUriPath();
        if (StringUtils.isBlank(uri)){
            uri="";
        }
        int _index = uri.indexOf("/v1/");
        if (_index!=-1){
            uri=uri.substring(_index);
        }
        out.write(uri.getBytes(PaymaxConfig.CHARSET));
        out.write('\n');//uri path
        out.write(request.getRequestQueryString().getBytes(PaymaxConfig.CHARSET));
        out.write('\n');//query string
        out.write(request.getHeaderValue(HEADER_KEY_NONCE).getBytes(PaymaxConfig.CHARSET));
        out.write('\n');//header
        out.write(request.getHeaderValue(HEADER_KEY_TIMESTAMP).getBytes(PaymaxConfig.CHARSET));
        out.write('\n');//header
        out.write(request.getHeaderValue(HEADER_KEY_AUTHORIZATION).getBytes(PaymaxConfig.CHARSET));
        out.write('\n');//header
        byte[] data = IOUtils.toByteArray(request.getRequestBody());
        out.write(data);//body
        out.close();
        String toSignString = out.toString(PaymaxConfig.CHARSET);
       // return RSA.sign(toSignString, SignConfig.PRIVATE_KEY);
        return RSA.sign(toSignString, private_key);
    }

}
