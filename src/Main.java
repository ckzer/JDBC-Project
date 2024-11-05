public class Main {
    public static void main(String[] args) {

        DBManage dbManage = new DBManage(); // DBManage 객체 생성 및 연결 시도

        // 연결이 성공적으로 이루어졌는지 확인하는 메시지 출력
        if (dbManage.getConnection() != null) {
            System.out.println("데이터베이스에 성공적으로 연결되었습니다.");
        } else {
            System.out.println("데이터베이스 연결에 실패했습니다.");
        }

        // 프로그램 종료 시 DB 연결 종료
        dbManage.close();
    }
}