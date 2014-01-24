import com.google.gson.*
import com.google.gson.stream.JsonReader

import java.net.URLDecoder
import java.awt.Color

import org.gradle.api.*

public class RamenDB {
	def static final crawlFile = "crawlRamenDB.json"
	
	def static crawl(def dataDir) {
		def extent = [139.6165,35.5591,139.9478,35.7836] //[139.76952688230722,35.67526190370757,139.77484838499277,35.67770218213608]
		double gridSize = 0.03
		def baseUrlForLinks = "http://supleks.jp/stuff/markers?site=ramendb&&count=1000&order=distance"
		def baseUrlForDetails = "http://supleks.jp/stuff/marker-info?site=ramendb"
		
		def nLon = Math.ceil((extent[2] - extent[0]) / gridSize)
		def nLat = Math.ceil((extent[3] - extent[1]) / gridSize)
		
		Map<String, Object> shops = new HashMap<String, Map<String, Object>>()

		for(int i = 0 ; i < nLon ; i++) {
			def left = extent[0] + gridSize * i
			def right = 0
			if(i == nLon - 1) {
				right = extent[2]
			} else {
				right = extent[0] + gridSize * (i+1)
			}
			def centerLon = (left + right) / 2
			
			for(int j = 0 ; j < nLat ; j++) {
				println "Processing cell ${i},${j}..."
				
				def bottom = extent[1] + gridSize * j
				def top = 0
				if(j == nLat - 1) {
					top = extent[3]
				} else {
					top = extent[1] + gridSize * (j + 1)
				}
				def centerLat = (bottom + top) / 2
				
				try {
					String url = baseUrlForLinks + "&top=" + top + "&bottom=" + bottom + "&left=" + left
					url += "&right=" + right + "&lng=" + centerLon + "&lat=" + centerLat
					
					String json = url.toURL().getText()
					
					JsonArray array = readRamenDBJson(json).getAsJsonArray()
					
					println "${array.size()} shops in cell..."
					for(int k = 0 ; k < array.size() ; k++) {
						def shop = array.get(k)
						def shopObject = new HashMap<String, Object>();
						def id = shop.getAsJsonPrimitive("i").getAsString()
						def lon = shop.getAsJsonPrimitive("x").getAsDouble()
						def lat = shop.getAsJsonPrimitive("y").getAsDouble()
						shopObject.put("id", id)
						shopObject.put("lat", lat)
						shopObject.put("lon", lon)
						
						shops.put(id, shopObject);
						
						try {
							println "Processing shop ${id}..."
							
							url = baseUrlForDetails + "&shop_id=" + id
							json = url.toURL().getText()
							
							def shopDetails = readRamenDBJson(json).getAsJsonObject()
							
							def name = shopDetails.getAsJsonPrimitive("name").getAsString()
							name = URLDecoder.decode(name, "UTF-8")
							println name
							def reviews = shopDetails.getAsJsonPrimitive("reviews").getAsInt()
							def average = shopDetails.getAsJsonPrimitive("average").getAsDouble()
							def score = shopDetails.getAsJsonPrimitive("point").getAsDouble()
							shopObject.put("name", name);
							shopObject.put("reviews", reviews)
							shopObject.put("average", average)
							shopObject.put("score", score)
							
							println "Success for shop ${id}!"
							
						} catch(def ex) {
							println "Error fetching details"
							ex.printStackTrace()
						}
					}
				} catch(def ex) {
					println "Error fetching grid cell " + i + "," + j
					ex.printStackTrace()
				}	
			}
		}
		
		outputJson(new File(dataDir, crawlFile), shops)
	}
	
	def static readRamenDBJson(String json) {
		def parser = new JsonParser()
		json = json.substring(1, json.length() - 2)
		def reader = new StringReader(json)
		def jsonReader = new JsonReader(reader)
		jsonReader.setLenient(true)
		def jsonObject = parser.parse(jsonReader)
		reader.close()
		return jsonObject
	}
	
	def static outputJson(File output, Object json) {
		output.withOutputStream {
			os ->
			def gson = new GsonBuilder().create();
			os << gson.toJson(json)
		}
	}
	
	def static analyze(def dataDir, String output) {
		def json = new File(dataDir, crawlFile).text
		def parser = new JsonParser()
		// Map with ids as keys
		def jsonObject = parser.parse(json).getAsJsonObject()
		def entries = jsonObject.entrySet()
		List<MapAnnotation> mapAnnotations = new ArrayList<MapAnnotation>()
		for(def entry in entries ) {
			def shopObject = entry.getValue().getAsJsonObject()
			MapAnnotation mapAnnotation = new MapAnnotation();
			mapAnnotation.id = shopObject.get("id").asString
			mapAnnotation.latitude = shopObject.get("lat").asDouble
			mapAnnotation.longitude = shopObject.get("lon").asDouble
			mapAnnotation.title = shopObject.get("name").asString
			def score = shopObject.get("score").asDouble
			def average = shopObject.get("average").asDouble
			def reviews = shopObject.get("reviews").asInt
			def description = "Score: ${score}\nAverage: ${average} (${reviews} reviews)"
			mapAnnotation.description = description
			if(score < 80) {
				mapAnnotation.color = Color.YELLOW.RGB
			} else {
				mapAnnotation.color = Color.GREEN.RGB
			}
			mapAnnotation.isBookmarked = false
			mapAnnotations.add(mapAnnotation);
		}
		
		outputJson(new File(dataDir, output), mapAnnotations)
		
	}
}

class MapAnnotation {
	public String id;
	public double latitude, longitude
	public String title, description
	public int color
	public boolean isBookmarked
}