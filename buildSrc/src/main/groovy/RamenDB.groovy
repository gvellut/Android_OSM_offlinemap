import com.google.gson.*
import com.google.gson.stream.JsonReader

import java.net.URLDecoder
import java.awt.Color

import org.gradle.api.*

public class RamenDB {
	def static final crawlFile = "crawlRamenDB.json"

	def static crawl(def dataDir) {
		def extent = [
			139.6165,
			35.5591,
			139.9478,
			35.7836]
		double gridSize = 0.03

		def nLon = Math.ceil((extent[2] - extent[0]) / gridSize)
		def nLat = Math.ceil((extent[3] - extent[1]) / gridSize)

		Map<String, Object> shops = new HashMap<String, Map<String, Object>>()

		queryRamenDB(extent, 0, nLon, 0, nLat, gridSize, shops)

		outputJson(new File(dataDir, crawlFile), shops)
		println "${shops.size()} shops in extent"
	}

	def static queryRamenDB(def extent, def sLon, def nLon, def sLat, def nLat, def gridSize, def outShops) {
		def baseUrlForLinks = "http://supleks.jp/stuff/markers?site=ramendb&count=100&order=distance"
		def baseUrlForDetails = "http://supleks.jp/stuff/marker-info?site=ramendb"
		// the url query for links does not return all points in extent
		// some points near the border are missing
		// so add some margin...
		def margin = 0.002

		for(int i = sLon ; i < nLon ; i++) {
			def left = extent[0] + gridSize * i
			def right = 0
			if(i == nLon - 1) {
				right = extent[2]
			} else {
				right = extent[0] + gridSize * (i+1)
			}
			def centerLon = (left + right) / 2

			for(int j = sLat ; j < nLat ; j++) {
				def bottom = extent[1] + gridSize * j
				def top = 0
				if(j == nLat - 1) {
					top = extent[3]
				} else {
					top = extent[1] + gridSize * (j + 1)
				}
				def centerLat = (bottom + top) / 2

				println "Processing cell ${i},${j} [${left},${bottom},${right},${top}]..."

				try {
					def queryTop = top
					def queryBottom = bottom
					def queryLeft = left
					def queryRight = right
					if(right - left > margin) {
						queryTop += margin
						queryBottom -= margin
						queryLeft -= margin
						queryRight += margin
					}
					String url = baseUrlForLinks + "&top=" + queryTop + "&bottom=" + queryBottom + "&left=" + queryLeft
					url += "&right=" + queryRight + "&lng=" + centerLon + "&lat=" + centerLat
					
					String json = url.toURL().getText()
					
					JsonArray array = readRamenDBJson(json).asJsonArray

					println "${array.size()} shops in cell..."
					
					if(array.size() >= 100) {
						// cannot query > 100: arg ignored by the server 
						// so subdivide when too many to make sure
						// we are not missing any shop
						
						println "Too many shops. Subdividing..."
						
						def newExtent = [left,bottom,right,top]
						def newNLon = 3
						def newNLat = 3
						def newGridSize = gridSize / 3
						queryRamenDB(newExtent, 0, newNLon, 0, newNLat, newGridSize, outShops)
						// jump to new grid cell
						continue
					}
					
					for(int k = 0 ; k < array.size() ; k++) {
						def shop = array.get(k)
						def shopObject = new HashMap<String, Object>()
						def id = shop.get("i").asString
						
						if(shopObject.containsKey(id)) {
							continue
						}
						
						def lon = shop.get("x").asDouble
						def lat = shop.get("y").asDouble
						shopObject.put("id", id)
						shopObject.put("lat", lat)
						shopObject.put("lon", lon)
						
						outShops.put(id, shopObject);

						try {
							url = baseUrlForDetails + "&shop_id=" + id
							json = url.toURL().getText()

							def shopDetails = readRamenDBJson(json).asJsonObject

							def name = shopDetails.get("name").asString
							name = URLDecoder.decode(name, "UTF-8")
							def reviews = Integer.parseInt(shopDetails.get("reviews").asString.replace(",", ""))
							def average = shopDetails.get("average").asDouble
							def score = shopDetails.get("point").asDouble
							shopObject.put("name", name);
							shopObject.put("reviews", reviews)
							shopObject.put("average", average)
							shopObject.put("score", score)
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
		output.withOutputStream { os ->
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
			try {
				MapAnnotation mapAnnotation = new MapAnnotation();
				mapAnnotation.id = shopObject.get("id").asString
				mapAnnotation.latitude = shopObject.get("lat").asDouble
				mapAnnotation.longitude = shopObject.get("lon").asDouble
				mapAnnotation.title = shopObject.get("name").asString.replace("&amp;", "&")
				def score = shopObject.get("score").asDouble
				def average = shopObject.get("average").asDouble
				def reviews = shopObject.get("reviews").asInt
				def description = "[\"${score}\",\"${average}\",\"${reviews}\"]"
				mapAnnotation.description = description
				if(score >= 80 || (average >= 80 && reviews >= 15)) {
					mapAnnotation.color = Color.GREEN.RGB
				} else {
					mapAnnotation.color = Color.YELLOW.RGB
				}
				mapAnnotation.isBookmarked = false
				mapAnnotations.add(mapAnnotation);

			} catch(Exception ex) {
				println "Error transforming annotation"
				ex.printStackTrace()
			}
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