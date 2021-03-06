package org.keycloak.audit.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.keycloak.audit.AuditProvider;
import org.keycloak.audit.Event;
import org.keycloak.audit.EventQuery;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MongoAuditProvider implements AuditProvider {

    private DBCollection audit;

    public MongoAuditProvider(DBCollection audit) {
        this.audit = audit;
    }

    @Override
    public EventQuery createQuery() {
        return new MongoEventQuery(audit);
    }

    @Override
    public void clear() {
        audit.remove(new BasicDBObject());
    }

    @Override
    public void clear(long olderThan) {
        audit.remove(new BasicDBObject("time", new BasicDBObject("$lt", olderThan)));
    }

    @Override
    public void onEvent(Event event) {
        audit.insert(convert(event));
    }

    @Override
    public void close() {
    }

    static DBObject convert(Event o) {
        BasicDBObject e = new BasicDBObject();
        e.put("time", o.getTime());
        e.put("event", o.getEvent());
        e.put("realmId", o.getRealmId());
        e.put("clientId", o.getClientId());
        e.put("userId", o.getUserId());
        e.put("ipAddress", o.getIpAddress());
        e.put("error", o.getError());

        BasicDBObject details = new BasicDBObject();
        for (Map.Entry<String, String> entry : o.getDetails().entrySet())  {
            details.put(entry.getKey(), entry.getValue());
        }
        e.put("details", details);

        return e;
    }

    static Event convert(BasicDBObject o) {
        Event e = new Event();
        e.setTime(o.getLong("time"));
        e.setEvent(o.getString("event"));
        e.setRealmId(o.getString("realmId"));
        e.setClientId(o.getString("clientId"));
        e.setUserId(o.getString("userId"));
        e.setIpAddress(o.getString("ipAddress"));
        e.setError(o.getString("error"));

        BasicDBObject d = (BasicDBObject) o.get("details");
        Map<String, String> details = new HashMap<String, String>();
        for (Object k : d.keySet()) {
            details.put((String) k, d.getString((String) k));
        }

        e.setDetails(details);
        return e;
    }

}
