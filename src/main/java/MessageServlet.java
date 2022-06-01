import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@WebServlet(urlPatterns = "/getMessage")
public class MessageServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject rspObj=new JSONObject();
        /**
         * 报文体返回值
         * 1，获取留言成功
         * -1，获取失败
         * 2，发表留言成功
         * -2，发表失败
         */
        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        int operType=reqPayload.getInteger("operType");
        /**
         * 操作类型
         * 1,获取留言
         * 2，发表留言
         * 3，删除留言
         */
        switch (operType){
            case 1:
                Set<String> uidSet=new HashSet<>();
                JSONArray messageListJsonArr=new JSONArray();
                int result=getMessage(reqPayload,uidSet,messageListJsonArr,rspObj);
                if(result==1){
                    result=getSenderUserInfo(uidSet,messageListJsonArr);
                    if(result!=0) rspObj.put("error",-1);
                    else {
                        rspObj.put("error",1);
                        rspObj.put("messageList", messageListJsonArr);
                    }
                }else rspObj.put("error",result);
                break;
            case 2:
                rspObj.put("error",leaveMessage(reqPayload));
                break;
        }

        rsp.setContentType("application/json;charset=utf-8");
        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    public int getMessage(JSONObject reqPayload, Set<String> uidSet, JSONArray messageListJsonArr, JSONObject rspObj){
        int result=-1;
        /**
         * 1，获取留言成功
         * -1，获取留言失败
         * 11，服务器运行失败
         */
        int page=reqPayload.getInteger("page");
        String receiverUid=reqPayload.getString("receiverUid");
        MysqlQuery q=new MysqlQuery();
        String limitStart=""+(page-1)*10;
        String sqlStr="select * from message where receiverUid='"+receiverUid+"' order by timeStamp DESC limit "+limitStart+",10";
        q.selectFromDb(sqlStr);
        try{
            while (q.rs.next()){
                String senderUid=q.rs.getString("senderUid");
                String content=q.rs.getString("content");
                String timeStamp=q.rs.getString("timeStamp");
                uidSet.add(senderUid);
                JSONObject tmpObj=new JSONObject();
                JSONObject senderObj=new JSONObject();
                senderObj.put("uid",senderUid);
                senderObj.put("timeStamp",timeStamp);
                tmpObj.put("sender",senderObj);
                tmpObj.put("content",content);
                messageListJsonArr.add(tmpObj);
            }
            sqlStr="select count(*) from message where receiverUid='"+receiverUid+"'";
            q.selectFromDb(sqlStr);
            if(q.rs.next()){
                rspObj.put("total",q.rs.getInt("count(*)"));
                result=1;
            }else result=-1;
        }catch (Exception e){
            e.printStackTrace();
            result=11;
        }
        q.closeDbConnection();
        return result;
    }

    public int getSenderUserInfo(Set<String> uidSet,JSONArray messageListJsonArr){
        int result=0;
        /**
         * 同new MysqlQuery.getUserBaseInfo的返回值
         */
        JSONObject uidMap=new JSONObject();
        for(Iterator<String> it = uidSet.iterator(); it.hasNext();){
            MysqlQuery q=new MysqlQuery();
            JSONObject tmpObj=new JSONObject();
            String uid=it.next();
            result=q.getUserBaseInfo(uid,tmpObj);
            if(result!=0) return result;
            uidMap.put(uid,tmpObj);
        }
        for(int i=0;i<messageListJsonArr.size();i++){
            JSONObject messageObj=messageListJsonArr.getJSONObject(i);
            JSONObject senderObj=messageObj.getJSONObject("sender");
            String uid=senderObj.getString("uid");
            JSONObject userObj=uidMap.getJSONObject(uid);
            senderObj.put("userName",userObj.getString("userName"));
            senderObj.put("avatarUrl",userObj.getString("avatarUrl"));
        }
        return result;
    }

    public int leaveMessage(JSONObject reqPayload){
        int result=-2;
        String senderUid=reqPayload.getString("senderUid");
        String reveiverUid=reqPayload.getString("receiverUid");
        String timeStamp=reqPayload.getString("timeStamp");
        String content=reqPayload.getString("content");
        MysqlQuery q=new MysqlQuery();
        String sqlStr="insert into message values(null,'"+senderUid+"','"+reveiverUid+"','"+content+"','"+timeStamp+"')";
        q.updateDbTable(sqlStr);
        result=q.rows>0? 2:-2;
        q.closeDbConnection();
        return result;
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }
}
