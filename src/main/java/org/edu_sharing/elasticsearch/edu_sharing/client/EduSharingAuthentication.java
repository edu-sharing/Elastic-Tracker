package org.edu_sharing.elasticsearch.edu_sharing.client;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.edu_sharing.elasticsearch.tracker.TransactionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.HttpURLConnection;
import java.util.Date;

@Aspect
@Component
public class EduSharingAuthentication {

    Logger logger = LoggerFactory.getLogger(EduSharingAuthentication.class);

    @Autowired
    private EduSharingClient eduSharingClient;

    long lastTimeAuthChecked = 0;

    @Value("${edusharing.sessionvalidate.delay}")
    long eduSharingSessionValidateDelay;

    @Before("@annotation(org.edu_sharing.elasticsearch.edu_sharing.client.EduSharingAuthentication.ManageAuthentication)")
    public void manageAuthentication(JoinPoint joinPoint) throws Throwable {
        Date d = new Date();
        if(lastTimeAuthChecked == 0 || (d.getTime() - lastTimeAuthChecked) > eduSharingSessionValidateDelay) {
            logger.debug("manageAuthentication for:" + joinPoint.getSignature().getName());
            eduSharingClient.manageAuthentication();
            lastTimeAuthChecked = d.getTime();
        }
    }


    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ManageAuthentication {

    }

    @Component
    public class EduSharingAuthenticationResponseFilter implements ClientResponseFilter{

        @Autowired
        EduSharingAuthentication auth;

        @Override
        public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
            if(responseContext.getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED){
                //force reauth
                logger.info("got "+responseContext.getStatus() +" force authentication");
                auth.lastTimeAuthChecked = 0;
            }
        }
    }

}


