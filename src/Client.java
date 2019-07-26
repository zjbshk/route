
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Logger;

public class Client {
    private final static byte ADD = 0b101;
    private final static byte REMOVE = 0b10;
    private final static int PORT = 8760;
    private static String toAddress;
    private static int toPort;


    private static String serverAddress;
    static Logger logger = Logger.getLogger(Client.class.getName());
    private static String SPLIT = "\n";


    public static void main(String[] args) {
        parse(args);
        start();
    }

    private static void parse(String[] args) {
        Map<String, String> map = new HashMap<>();
        map.put("-ta", "toAddress");
        map.put("-tp", "toPort");
        map.put("-sa", "serverAddress");
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (map.containsKey(arg)) {
                System.setProperty(map.remove(arg), args[++i]);
            }
        }
        if (map.size() != 0) {
            throw new RuntimeException("解析异常，请检查参数是否正确!");
        }
        toAddress = System.getProperty("toAddress");
        toPort = Integer.parseInt(System.getProperty("toPort"));
        serverAddress = System.getProperty("serverAddress");
    }

    private static void start() {
        try {
            SocketChannel scToServer = SocketChannel.open();
            scToServer.connect(new InetSocketAddress(serverAddress, PORT));
            scToServer.configureBlocking(false);
//            SocketChannel scToServer = SocketChannel.open(new InetSocketAddress(serverAddress, PORT));


            Selector selector = Selector.open();
            scToServer.register(selector, SelectionKey.OP_READ);

            logger.info("<<< Client 服务初始化完毕，开始select  >>>");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (scToServer.isConnected()) {
                        try {
                            scToServer.write(ByteBuffer.wrap(new byte[]{ADD}));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 1000, new Date().getTime());

            while (selector.select() > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey sk = iterator.next();
                    if (sk.isReadable()) {
                        logger.info("<<< Client 有读操作 >>>");

                        ByteBuffer allocate = ByteBuffer.allocate(1024);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        do {
                            allocate.flip();
                            byteArrayOutputStream.write(allocate.array(), allocate.position(), allocate.limit());
                            allocate.clear();
                        } while (scToServer.read(allocate) != 0);
                        String html = byteArrayOutputStream.toString();
                        byteArrayOutputStream.close();

                        int start = html.indexOf("\n");
                        String id = html.substring(0, start);
                        String data = html.substring(start + 1);

                        System.out.println(html);
                        SocketChannel scTo = SocketChannel.open();
                        scTo.connect(new InetSocketAddress(toAddress, toPort));
                        scTo.write(ByteBuffer.wrap(data.getBytes()));


//                        byteArrayOutputStream = new ByteArrayOutputStream();
//                        do {
//                            allocate.flip();
//                            byteArrayOutputStream.write(allocate.array(), allocate.position(), allocate.limit());
//                            allocate.clear();
//                        } while (channel.read(allocate) != 0);
//                        html = byteArrayOutputStream.toString();
//                        byteArrayOutputStream.close();

//                         data = String.format("%s%s%s", id, SPLIT, html);
//                        byte[] dataPackage = data.getBytes();
                        byte[] bytes = (id + "\n").getBytes();
                        boolean isFirst = false;
                        while (scTo.read(allocate) != 0) {
                            if (!isFirst) {
                                scToServer.write(ByteBuffer.wrap(bytes));
                                isFirst = true;
                            }
                            allocate.flip();
                            System.out.println(new String(allocate.array(), allocate.position(), allocate.limit()));
                            scToServer.write(allocate);
                            allocate.clear();
                        }

                        scTo.finishConnect();
                        scTo.close();
                    } else {
                        System.out.println("<<< default >>>");
                    }
                    iterator.remove();
                }
            }


            System.out.println("<<<  Over  >>>");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
