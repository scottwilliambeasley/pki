//
// Copyright Red Hat, Inc.
//
// SPDX-License-Identifier: GPL-2.0-or-later
//
package org.dogtagpki.acme.server;

import java.math.BigInteger;
import java.net.URI;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.codec.binary.Base64;
import org.dogtagpki.acme.ACMEAccount;
import org.dogtagpki.acme.ACMEHeader;
import org.dogtagpki.acme.ACMENonce;
import org.dogtagpki.acme.ACMEOrder;
import org.dogtagpki.acme.JWS;
import org.dogtagpki.acme.backend.ACMEBackend;

/**
 * @author Endi S. Dewata
 */
@Path("order/{id}/finalize")
public class ACMEFinalizeOrderService {

    public static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ACMEFinalizeOrderService.class);

    @Context
    UriInfo uriInfo;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response handlePOST(@PathParam("id") String orderID, JWS jws) throws Exception {

        logger.info("Finalizing order " + orderID);

        String protectedHeader = new String(jws.getProtectedHeaderAsBytes(), "UTF-8");
        logger.info("Header: " + protectedHeader);
        ACMEHeader header = ACMEHeader.fromJSON(protectedHeader);

        ACMEEngine engine = ACMEEngine.getInstance();
        engine.validateNonce(header.getNonce());

        URI kid = header.getKid();
        String kidPath = kid.getPath();
        String accountID = kidPath.substring(kidPath.lastIndexOf('/') + 1);
        logger.info("Account ID: " + accountID);

        ACMEAccount account = engine.getAccount(accountID);
        engine.validateJWS(jws, header.getAlg(), account.getJWK());

        String payload = new String(jws.getPayloadAsBytes(), "UTF-8");
        logger.info("Payload: " + payload);

        ACMEOrder order = engine.getOrder(account, orderID);

        if (!order.getStatus().equals("ready")) {
            // TODO: generate proper exception
            throw new Exception("Order not ready: " + orderID);
        }

        order.setStatus("processing");
        engine.updateOrder(account, order);

        ACMEOrder request = ACMEOrder.fromJSON(payload);

        String csr = request.getCSR();
        logger.info("CSR: " + csr);
        engine.validateCSR(account, order, csr);

        ACMEBackend backend = engine.getBackend();
        BigInteger serialNumber = backend.issueCertificate(csr);
        String certID = Base64.encodeBase64URLSafeString(serialNumber.toByteArray());

        logger.info("Certificate issued: " + certID);

        order.setStatus("valid");
        order.setSerialNumber(serialNumber);

        engine.updateOrder(account, order);

        URI finalizeURL = uriInfo.getBaseUriBuilder().path("order").path(orderID).path("finalize").build();
        order.setFinalize(finalizeURL);

        URI certURL = uriInfo.getBaseUriBuilder().path("cert").path(certID).build();
        order.setCertificate(certURL);

        ResponseBuilder builder = Response.ok();

        ACMENonce nonce = engine.createNonce();
        builder.header("Replay-Nonce", nonce.getValue());

        URI index = uriInfo.getBaseUriBuilder().path("directory").build();
        builder.link(index, "index");

        builder.entity(order);

        return builder.build();
    }
}
