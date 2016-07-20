package nl.tno.stormcv.operation;


import backtype.storm.task.TopologyContext;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.jersey.api.container.httpserver.HttpServerFactory;
import com.sun.jersey.api.core.ApplicationAdapter;
import com.sun.net.httpserver.HttpServer;
import nl.tno.stormcv.model.CVParticle;
import nl.tno.stormcv.model.Frame;
import nl.tno.stormcv.model.serializer.CVParticleSerializer;
import nl.tno.stormcv.model.serializer.FrameSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Aetf (aetf at unlimitedcodeworks dot xyz) on 16-7-19.
 */
@Path("/results")
public class ResultSinkOp extends Application
        implements ISingleInputOperation<Frame>, IBatchOperation<Frame> {

    private static final long serialVersionUID = -4658017042873627826L;
    private static Cache<String, List<CVParticle>> results = null;
    private Logger logger = LoggerFactory.getLogger(getClass());

    private HttpServer server;
    private int port = 8558;
    private int topNumber = 10;
    private CVParticleSerializer<Frame> serializer = new FrameSerializer();

    private static Cache<String, List<CVParticle>> getResults() {
        if (results == null) {
            results = CacheBuilder.newBuilder()
                    .expireAfterWrite(20, TimeUnit.SECONDS)
                    .build();
        }
        return results;
    }

    public ResultSinkOp port(int nr) {
        this.port = nr;
        return this;
    }

    public ResultSinkOp topNumber(int nr) {
        this.topNumber = nr;
        return this;
    }

    /**
     * Sets the classes to be used as resources for this application
     */
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> s = new HashSet<>();
        s.add(ResultSinkOp.class);
        return s;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context) throws Exception {
        results = ResultSinkOp.getResults();

        ApplicationAdapter connector = new ApplicationAdapter(this);
        server = HttpServerFactory.create("http://0.0.0.0:" + port + "/", connector);
        server.start();
    }

    @Override
    public void deactivate() {
        server.stop(0);
        results.invalidateAll();
        results.cleanUp();
    }

    @Override
    public CVParticleSerializer<Frame> getSerializer() {
        return serializer;
    }

    @Override
    public List<Frame> execute(CVParticle input) throws Exception {
        List<CVParticle> list = results.getIfPresent(input.getStreamId());
        if (list == null) {
            list = new ArrayList<>();
            results.put(input.getStreamId(), list);
        }

        list.add(input);
        while (list.size() > topNumber) {
            list.remove(0);
        }
        return new ArrayList<>();
    }

    @Override
    public List<Frame> execute(List<CVParticle> input) throws Exception {
        for (CVParticle s : input) {
            execute(s);
        }
        return new ArrayList<>();
    }

    @GET
    @Path("/")
    @Produces(MediaType.TEXT_HTML)
    public String getStreamIds() throws IOException {
        /*
        String result = "";
        for (String id : results.asMap().keySet()) {
            result += "/results/list/" + id + "\r\n";
        }
        return result;
        */

        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>All Stream ");
        sb.append("Results</title></head><body bgcolor=\"#3C3C3C\">");
        sb.append("<font style=\"color:#CCC;\">Total: ");
        sb.append(results.asMap().size());
        sb.append("<br />");
        for (String id : results.asMap().keySet()) {
            sb.append("<a href=\"/results/top?streamid=");
            sb.append(URLEncoder.encode(id, "UTF-8"));
            sb.append("\">");
            sb.append(id);
            sb.append("</a> ");
            sb.append("<br />");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    @GET
    @Path("top")
    @Produces(MediaType.TEXT_HTML)
    public String resultList(@QueryParam("streamid") final String streamId) {
        List<CVParticle> streamResult;
        if ((streamResult = results.getIfPresent(streamId)) != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><title>Recent ");
            sb.append(streamResult.size());
            sb.append("Results</title></head><body bgcolor=\"#3C3C3C\">");
            sb.append("<font style=\"color:#CCC;\">StreamId: ");
            sb.append(streamId);
            sb.append("<br />");
            for (CVParticle cvt : streamResult) {
                sb.append(cvt);
                sb.append("<br />");
            }
            sb.append("</body></html>");
            return sb.toString();
        } else {
            return "Stream not found";
        }
    }
}
