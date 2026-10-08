package dev.boutique.orders;
import com.fasterxml.jackson.databind.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;import java.net.http.*;import java.time.Duration;import java.util.*;
@RestController
public class OrdersController {
 private final JdbcTemplate db;private final ObjectMapper mapper;
 private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
 private final String catalogue=System.getenv().getOrDefault("CATALOGUE_URL","http://localhost:8081"),cart=System.getenv().getOrDefault("CART_URL","http://localhost:8082");
 public OrdersController(JdbcTemplate db,ObjectMapper mapper){this.db=db;this.mapper=mapper;}
 static void validateSession(String s){if(s==null||!s.matches("[a-f0-9]{64}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalid_session");}
 static void validateKey(String s){if(s==null||!s.matches("[A-Za-z0-9_-]{8,64}"))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"invalid_key");}
 private JsonNode get(String url,String rid){try{var res=http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(3)).header("X-Request-ID",rid).GET().build(),HttpResponse.BodyHandlers.ofString());if(res.statusCode()!=200)throw new IllegalStateException();return mapper.readTree(res.body());}catch(InterruptedException e){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);}}
 private Map<String,Object> decode(Map<String,Object> row){try{row.put("items",mapper.readTree(row.get("items").toString()));return row;}catch(Exception e){throw new IllegalStateException(e);}}
 private List<Map<String,Object>> find(String session,String key){return db.queryForList("SELECT id,total,currency,items,created_at FROM orders WHERE session_id=? AND idempotency_key=?",session,key);}
 @GetMapping("/health/live") public Map<String,String> live(){return Map.of("status","ok");}
 @GetMapping("/health/ready") public Map<String,String> ready(){db.queryForObject("SELECT 1",Integer.class);return Map.of("status","ok");}
 @PostMapping("/orders") @Transactional(timeout=15)
 public Map<String,Object> create(@RequestHeader("X-Session-ID")String session,@RequestHeader("Idempotency-Key")String key,@RequestHeader(value="X-Request-ID",defaultValue="unknown")String rid){
  validateSession(session);validateKey(key);db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",session+":"+key);
  var old=find(session,key);if(!old.isEmpty())return decode(old.getFirst());
  var requested=get(cart+"/carts/"+session,rid).path("items");if(!requested.isArray()||requested.isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"empty_cart");
  var prices=new HashMap<String,JsonNode>();get(catalogue+"/products",rid).forEach(p->prices.put(p.path("id").asText(),p));
  var items=new ArrayList<Map<String,Object>>();int total=0;
  for(var i:requested){String id=i.path("productId").asText();int q=i.path("quantity").asInt();var p=prices.get(id);if(p==null||q<1||q>20)throw new ResponseStatusException(HttpStatus.CONFLICT,"invalid_cart");int price=p.path("price").asInt();if(price<=0||!p.path("currency").asText().equals("USD"))throw new ResponseStatusException(HttpStatus.CONFLICT);total=Math.addExact(total,Math.multiplyExact(price,q));items.add(Map.of("productId",id,"name",p.path("name").asText(),"quantity",q,"price",price));}
  try{db.update("INSERT INTO orders(id,session_id,idempotency_key,total,currency,items) VALUES (?,?,?,?,?,?::jsonb)",UUID.randomUUID(),session,key,total,"USD",mapper.writeValueAsString(items));}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
  return decode(find(session,key).getFirst());
 }
 @GetMapping("/orders/{id}") public Map<String,Object> read(@PathVariable UUID id,@RequestHeader("X-Session-ID")String session){validateSession(session);var rows=db.queryForList("SELECT id,total,currency,items,created_at FROM orders WHERE id=? AND session_id=?",id,session);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND);return decode(rows.getFirst());}
}
