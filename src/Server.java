
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Logger;

public class Server {

    private final static byte ADD = 0b101;
    private final static byte REMOVE = 0b10;
    private final static int PORT = 8760;
    private static List<SocketChannel> scList = new ArrayList<>();


    private static Logger logger = Logger.getLogger(Server.class.getName());
    private static Random random = new Random();
    private static Map<String, SocketChannel> requestMap = new HashMap<>();
    private static String SPLIT = "\n";


    public static void main(String[] args) {
        start(args);
    }

    private static void start(String[] args) {
        try {
            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(PORT));
            ssc.configureBlocking(false);
            Selector selector = Selector.open();
            ssc.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("<<< Server 服务初始化完毕，开始select  >>>");

            while (selector.select() != 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey sk = iterator.next();
                    if (sk.isReadable()) {
                        logger.info("<<< Server 有读操作 >>>");

                        SocketChannel channel = (SocketChannel) sk.channel();

                        ByteBuffer allocate = ByteBuffer.allocate(1024);
//                        try {
//                            channel.read(allocate);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                            channel.write(ByteBuffer.wrap(new byte[]{REMOVE}));
//                            channel.finishConnect();
//                            channel.close();
//                            iterator.remove();
//                            continue;
//                        }


                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        do {
                            allocate.flip();
                            byteArrayOutputStream.write(allocate.array(), allocate.position(), allocate.limit());
                            allocate.clear();
                        } while (channel.read(allocate) != 0);
                        byte[] bytes = byteArrayOutputStream.toByteArray();
                        byteArrayOutputStream.close();

                        if (bytes.length == 1) {
                            byte tmp = allocate.get(0);
                            if (tmp == ADD) {
                                scList.add(channel);
                                logger.info("添加一个channel：" + channel.getRemoteAddress());
                            } else if (tmp == REMOVE) {
                                scList.remove(channel);
                                logger.info("删除一个channel：" + channel.getRemoteAddress());
                            } else {
                                channel.write(ByteBuffer.wrap(("Code Error," + tmp + " is Not").getBytes()));
                            }
                            iterator.remove();
                            continue;
                        }

                        String html = new String(bytes);

                        System.out.println(html);
                        if (isIn(channel, scList)) {
                            int start = html.indexOf(SPLIT);
                            String id = html.substring(0, start);

                            logger.info("id=" + id);
                            logger.info(Arrays.toString(requestMap.keySet().toArray()));
                            if (requestMap.containsKey(id)) {
                                SocketChannel socketChannel = requestMap.get(id);
                                socketChannel.write(ByteBuffer.wrap(html.substring(start + 1).getBytes()));
                            }
                        } else {
                            int randomInt = random.nextInt(Integer.MAX_VALUE);
                            String id = String.valueOf(randomInt);
                            requestMap.put(id, channel);

                            String data = String.format("%s%s%s", id, SPLIT, html);
                            byte[] dataPackage = data.getBytes();

                            Iterator<SocketChannel> scIterator = scList.iterator();
                            while (scIterator.hasNext()) {
                                SocketChannel socketChannel = scIterator.next();
                                if (socketChannel.isConnected()) {
                                    socketChannel.write(ByteBuffer.wrap(dataPackage));
                                } else {
                                    scIterator.remove();
                                }
                            }
                        }
                    } else if (sk.isAcceptable()) {
                        SocketChannel accept = ssc.accept();
                        accept.configureBlocking(false);
                        accept.register(selector, SelectionKey.OP_READ);
                    } else {
                        System.out.println("<<< default >>>");
                    }
                    iterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static boolean isIn(SocketChannel channel, List<SocketChannel> scList) {
        for (SocketChannel socketChannel : scList) {
            if (channel == socketChannel) {
                return true;
            }
        }
        return false;
    }

}
