FROM maven as build 

COPY . . 
RUN mvn clean package


FROM gcr.io/distroless/java:11 

COPY --from=build /target/*.jar tracker.jar

CMD ["tracker.jar"]
