package Server;

import Core.PerformanceMonitor;
import com.sun.javafx.perf.PerformanceTracker;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * @author: Minghao Liu
 */
public class MainServer {
    public static void main(String[] args) throws IOException {
        System.out.println("Server started!!!");
        InetSocketAddress address = new InetSocketAddress(1031);
        HttpServer httpServer = HttpServer.create(address, 0);
        httpServer.createContext("/", new HomePageHandler());
        httpServer.createContext("/loadBalancing", new BalancingHandler());
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.start();
    }
}
