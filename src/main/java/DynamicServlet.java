import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

@WebServlet(urlPatterns = "/getDynamic")
public class DynamicServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject rspObj=new JSONObject();
        /**
         * 报文体返回值
         * 1，获取动态成功
         * -1，获取失败
         * 2，发表动态成功
         * -2，发表失败
         * 3，删除动态成功
         * -3.删除动态失败
         */
        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        int operType=reqPayload.getInteger("operType");
        /**
         * 1，获取所有动态
         * 2，发表动态
         * 3，删除动态
         */
        switch (operType){
            case 1:
                JSONArray dynamicListJsonArr=new JSONArray();
                rspObj.put("error",getDynamic(reqPayload,dynamicListJsonArr,rspObj));
                rspObj.put("dynamicList",dynamicListJsonArr);
                break;
            case 2:
                String uid=reqPayload.getString("uid");
                String content=reqPayload.getString("content");
                String timeStamp=reqPayload.getString("timeStamp");
                MysqlQuery q=new MysqlQuery();
                String sqlStr="insert into dynamic values(null,'"+uid+"','"+content+"','"+timeStamp+"')";
                q.updateDbTable(sqlStr);
                rspObj.put("error",q.rows>0? 2:-2);
                q.closeDbConnection();
                break;
            case 3:
                int dynamicId=reqPayload.getInteger("dynamicId");
                q=new MysqlQuery();
                sqlStr="delete from dynamic where dynamicId="+dynamicId;
                q.updateDbTable(sqlStr);
                rspObj.put("error",q.rows>0? 3:-3);
                q.closeDbConnection();
                break;
        }
        rsp.setContentType("application/json;charset=utf-8");
        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    public int getDynamic(JSONObject reqPayload, JSONArray dynamicListJsonArr, JSONObject rspObj){
        int result=-1;
        /**
         * 1，获取动态成功
         * -1，获取失败
         * 11，服务器运行失败
         */
        int page=reqPayload.getInteger("page");
        String uid=reqPayload.getString("uid");
        MysqlQuery q=new MysqlQuery();
        String limitStart=""+(page-1)*10;
        String sqlStr="select * from dynamic where uid='"+uid+"' order by timeStamp DESC limit "+limitStart+",10";
        q.selectFromDb(sqlStr);
        try{
            while (q.rs.next()){
                String content=q.rs.getString("content");
                String timeStamp=q.rs.getString("timeStamp");
                int id=q.rs.getInt("dynamicId");
                JSONObject tmpObj=new JSONObject();
                tmpObj.put("content",content);
                tmpObj.put("timeStamp",timeStamp);
                tmpObj.put("id",id);
                dynamicListJsonArr.add(tmpObj);
            }
            sqlStr="select count(*) from dynamic where uid='"+uid+"'";
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

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }
}
