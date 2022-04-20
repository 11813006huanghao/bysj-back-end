import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpSender {

    public static void main(String[] args) throws Exception {
        System.out.println(post("http://jmsms.market.alicloudapi.com/sms/send?mobile=15673517897&templateId=M72CB42894&value=15",""));
    }

    public static int post(String url, String params) {
        try {
            URL httpUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) httpUrl.openConnection();
            //connection.setRequestProperty("content-type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Authorization", "APPCODE bbdcdbd466e94ef5a4f72a85bcc9a36d");
            //connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.connect();

                StringBuilder content = new StringBuilder();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String str;
                while ((str = bufferedReader.readLine()) != null) {
                    content.append(str);
                }
                bufferedReader.close();
                System.out.println(content.toString());

            return connection.getResponseCode();
        }catch (Exception e){
            System.out.println(e);
            return -100; //exception happened
        }
//        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
//        bufferedWriter.write(params);
//        bufferedWriter.flush();
//        bufferedWriter.close();
        //StringBuilder content = new StringBuilder();
//        if (connection.getResponseCode() == 200) {
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//            String str;
//            while ((str = bufferedReader.readLine()) != null) {
//                content.append(str);
//            }
//            bufferedReader.close();
//            return content.toString();
//        } else {
//            System.err.println(connection.getResponseCode());
//        }
    }
}
