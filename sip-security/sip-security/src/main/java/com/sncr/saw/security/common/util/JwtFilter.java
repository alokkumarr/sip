package com.sncr.saw.security.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sncr.saw.security.app.service.TicketHelper;
import com.synchronoss.bda.sip.jwt.token.RoleType;
import com.synchronoss.bda.sip.jwt.token.Ticket;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.GenericFilterBean;

@Service
public class JwtFilter extends GenericFilterBean {

  private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

  private static String SIP_AUTH = "/sip-security/auth";

  private final String jwtSecretKey;
  private final TicketHelper ticketHelper;
  private static final ObjectMapper mapper = new ObjectMapper();

  public JwtFilter(String jwtSecretKey, TicketHelper ticketHelper) {
    this.jwtSecretKey = jwtSecretKey;
    this.ticketHelper = ticketHelper;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException, ServletException {
    final HttpServletRequest request = (HttpServletRequest) req;
    final HttpServletResponse response = (HttpServletResponse) res;
    boolean haveInValidFlow = false;
    String errorMessage = null;

    if (!("OPTIONS".equals(request.getMethod()))) {
      final String authHeader = request.getHeader("Authorization");
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new ServletException("Missing or invalid Authorization header.");
      }

      final String token = authHeader.substring(7); // The part after Bearer
      Claims claims = null;
      try {
        claims =
            Jwts.parser()
                .setSigningKey(jwtSecretKey)
                .parseClaimsJws(token)
                .getBody();
        request.setAttribute("claims", claims);
      } catch (final SignatureException e) {
        throw new ServletException("Invalid token.");
      } catch (MalformedJwtException ex) {
        throw new ServletException("Invalid token");
      } catch (ExpiredJwtException expired) {
        throw new ServletException("token has expired");
      }

      // This checks the validity of the token. logging out does not need
      // the token to be active.
      String requestURI = request.getRequestURI();
      logger.trace("Request Header URI : " + requestURI);
      if (!requestURI.equals(SIP_AUTH + "/doLogout")) {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Ticket ticket = mapper.convertValue(claims.get("ticket"), Ticket.class);
        if (!ticket.isValid()) {
          haveInValidFlow = true;
          errorMessage = "Token has expired. Please re-login.";
        } else if (requestURI.startsWith(SIP_AUTH + "/admin") && !ticket.getRoleType().equals(RoleType.ADMIN)) {
          haveInValidFlow = true;
          errorMessage = "You are not authorized to perform this operation.";
        } else if (!(ticket.getTicketId() != null && ticketHelper.checkTicketValid(ticket.getTicketId(), ticket.getMasterLoginId()))) {
          haveInValidFlow = true;
          errorMessage = "Token is not valid.";
        }
      }
    }
    if (haveInValidFlow) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, errorMessage);
    } else {
      chain.doFilter(req, response);
    }
  }
}
