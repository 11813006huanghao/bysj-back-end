import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = "/checkEntityExist")
public class CheckEntityExistServlet extends HttpServlet {

    protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws IOException {
        Util.allowCrossOrigin(rsp);
        JSONObject rspObj=new JSONObject();
        /**
         * 1，实体存在
         * -1，实习不存在
         */

        JSONObject reqPayload= JSON.parseObject( Util.parsePostPayload(req));
        String entity=reqPayload.getString("entity");
        String id=reqPayload.getString("id");
        MysqlQuery q=new MysqlQuery();
        String sqlStr="";
        if("user".equals(entity)) sqlStr="select * from user where uid='" + id + "'";
        else if("game".equals(entity)) sqlStr="select * from game where gid='"+id+"'";
        q.selectFromDb(sqlStr);
        try {
            rspObj.put("error",q.rs.next()?1:-1);
        }catch (Exception e){
            e.printStackTrace();
        }
        q.closeDbConnection();
        rsp.setContentType("application/json;charset=utf-8");
        PrintWriter pw=rsp.getWriter();
        pw.write(rspObj.toJSONString());
        pw.flush();
    }

    protected void doOptions(HttpServletRequest req,HttpServletResponse rsp){
        Util.allowCrossOrigin(rsp);
    }
}
