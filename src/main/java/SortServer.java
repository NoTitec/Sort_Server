import java.net.*;
import java.io.*;
import java.util.Arrays;

public class SortServer{
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException{
        ServerSocket sSocket = new ServerSocket(3000);
        System.out.println("클라이언트 접속 대기중...");
        Socket socket = sSocket.accept();
        System.out.println("클라이언트 접속");

        // 바이트 배열로 전송할 것이므로 필터 스트림 없이 Input/OutputStream만 사용해도 됨
        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();

        // 클라이언트에게 문자열 정보 요청용 프로토콜 객체 생성 및 전송
        Sort_Protocol protocol = new Sort_Protocol(Sort_Protocol.PT_REQ_INPUT);
        os.write(protocol.getPacket());//클라이언트가 패킷보낼때까지 대기하다가 수신

        boolean program_stop = false;//프로그램 종료 판단 변수

        while(true){
            protocol = new Sort_Protocol();			// 새 Protocol 객체 생성 (기본 생성자)
            byte[] buf = protocol.getPacket();	// buf 기본생성자 packet byte배열 정보로 초기화

            is.read(buf);						// 클라이언트로부터 문자열 입력한 패킷받음 10005바이트 읽고 buf에 저장
            int packetType = buf[0];			// 수신 데이터에서 패킷 타입 얻음
            protocol.setPacket(packetType,buf);	// 패킷 타입에따라 Protocol 객체의 packet 만들고 buf 크기 변경

            switch(packetType){
                case Sort_Protocol.PT_EXIT:			// 프로그램 종료 수신
                    protocol = new Sort_Protocol(Sort_Protocol.PT_EXIT);//종료 패킷 생성
                    os.write(protocol.getPacket());//종료 패킷 전송
                    program_stop = true;//프로그램 종료 변수 true 변경
                    System.out.println("서버종료");
                    break;

                case Sort_Protocol.PT_RES_INPUT:		// 문자열 정보 수신
                    System.out.println("클라이언트가 " + "문자열 정보를 보냈습니다");

                    byte[] slice;//클라이언트가 보낸 word개수byte정보저장할 byte 배열
                    byte[] slicecount = Arrays.copyOfRange(protocol.getPacket(), 1, 5);//가져온 패킷의 단어개수저장된 1~5byte 읽음
                    slice=slicecount;
                    int inwordcount=protocol.byteArrayToInt(slice);//4byte를 int전환하여 수신받은 단어 개수 추출
                    System.out.println("수신한 개수:"+inwordcount+"개");
                    String[] temp=new String[inwordcount];//패킷에서 추출한 단어저장할 배열
                    //protocol객체 packet배열에서 단어 추출해서 temp 배열에 넣음
                    int pos=6;//패킷타입 1byte+단어개수 4byte 따라서 첫번째 단어 길이정보시작은 6

                    //단어길이 int 값얻고 그 길이만큼 byte 배열 읽어서 string word 얻는걸 반복
                    for(int i=0;i<inwordcount;i++){
                        byte[] cbuf=Arrays.copyOfRange(protocol.getPacket(), pos, pos+4);//one word 단어 길이 추출해 저장
                        int wordlength=protocol.byteArrayToInt(cbuf);//4byte 길이정보 int 변환
                        pos+=4;//4byte 읽었으므로 pos를 4만큼 증가

                        byte[] word = Arrays.copyOfRange(protocol.getPacket(), pos, pos+wordlength);//추출한길이만큼읽어 단어추출
                        String onestr = new String(word);//추출 word를  string으로 변환하여 저장

                        temp[i]=onestr;//temp에 저장
                        pos+=wordlength;// one word길이만큼 읽었으므로 pos를 one word 길이만큼 증가
                    }
                    System.out.println("클라이언트가보낸 word들 저장 완료");

                    Arrays.sort(temp);//temp 배열 오름차순 정렬
                    System.out.println("수신받은 words 정렬완료");
                    String[] sorted=temp;//temp를 정렬한 결과 저장할 string 배열


                    protocol=new Sort_Protocol(Sort_Protocol.PT_INPUT_RESULT);//정렬 결과 패킷 생성
                    protocol.setsorteddata(sorted);//생성된 패킷에 정렬된 data 넣기

                    System.out.println("정렬 문자열 정보 전송");
                    os.write(protocol.getPacket());//완성된 패킷 전송


            }//end switch

            if(program_stop) break;//종료타입 변수가 true이면 while문 종료하고 종료

        }//end while

        is.close();
        os.close();
        socket.close();
    }
}