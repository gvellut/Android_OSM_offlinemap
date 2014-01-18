import org.gradle.api.*

public class OFMUtils {
	
	public static def loadProperties(Project project, String propertiesFilePath) {
		
		def props = new Properties()
		project.file(propertiesFilePath).withInputStream {
			stream -> props.load(stream)
		}
		props.stringPropertyNames().collect { project.ext[it] = props.getProperty(it)}
	}
	
}