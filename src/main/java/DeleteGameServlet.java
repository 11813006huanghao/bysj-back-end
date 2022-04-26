import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/deleteGame")
public class DeleteGameServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject rspObj=new JSONObject();
        /**
         * 返回体
         * 1，删除成功
         * -1，删除失败
         */
        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        String gid=reqPayload.getString("gid");
        MysqlQuery q=new MysqlQuery();
        String sqlStr="delete from game where gid='"+gid+"'";
        q.updateDbTable(sqlStr);
        rspObj.put("error",q.rows>0? 1:-1);
        q.closeDbConnection();
        rsp.setContentType("application/json");
        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }
}
