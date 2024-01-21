package REST.server.computation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/chordnode")
public interface ChordNodeInterface {

    @GET
    @Path("/join")
    @Produces({ MediaType.APPLICATION_JSON })
    String join(@QueryParam("newNodeAddress") String newNodeAddress);

    @GET
    @Path("/leave")
    @Produces({ MediaType.APPLICATION_JSON })
    String leave();

    @GET
    @Path("/lookup")
    @Produces({ MediaType.APPLICATION_JSON })
    String lookup(@QueryParam("key") String key);

    @GET
    @Path("/sendMessage")
    @Produces({ MediaType.APPLICATION_JSON })
    String sendMessage(
            @QueryParam("destinationNode") String destinationNode,
            @QueryParam("message") String message
    );

}
