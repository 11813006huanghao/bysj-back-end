import java.sql.*;

public class MysqlQuery {
    static String dbUrl="jdbc:mysql://116.205.171.68:3306/hh_gamer";
    static String dbUserName="hh";
    static String dbPwd="722248hh";

    public Connection conn;
    public Statement stat;
    public ResultSet rs;
    public int rows=-1;

    public void selectFromDb(String sqlStr){
        if(!init()) return;
        try {
            rs = stat.executeQuery(sqlStr);
        }catch (Exception e){
            System.out.println(e);
            return;
        }
    }

    public void updateDbTable(String sqlStr){
       if(!init()) return;
        try {
            rows = stat.executeUpdate(sqlStr);
        }catch (Exception e){
            System.out.println(e);
            return;
        }
    }

    public void closeDbConnection(){
        try {
            if (rs != null) rs.close();
            if (stat != null) stat.close();
            if (conn != null) conn.close();
        }catch (Exception e){
            System.out.println(e);
        }
    }

    public boolean init(){
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }catch(ClassNotFoundException e1){
            System.out.println("驱动程序未找到");
            return false;
        }
        try {
            conn = DriverManager.getConnection(dbUrl, dbUserName, dbPwd);
            stat = conn.createStatement();
        }catch (Exception e){
            System.out.println(e);
            return false ;
        }
        return true;
    }
}
