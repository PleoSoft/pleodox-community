
PleoDox is a smart, scalable and robust document templating solution that automates all of the sensitive field-filling grunt work, enabling you to pursue a more worthwhile use of office time. 
Once the setup is done, you will be able to generate dozens of documents within minutes without the fear of that one little typo in your address, your VAT ID or bank account number that could 
cost you embarrassment or even serious fines if not caught in time.

# Run Locally
	- git checkout
	- you will need libreoffice (used with jod converter)
	- setup up the libreoffice path in application.properties
		ie: "jodconverter.local.office-home=/YOUR_PATH_TO_libreoffice/App/libreoffice"
	- you will need Java 11 (although JDK 8 should work)
	- start com.pleosoft.pleodox.community.PleodoxCommunityApplication

- Spring boot will start tomcat on the port 8080
- Jodconverter will start libreoffice on the port 8100
- Sample templates are configured at 
	- pleodox.storage.templatesDir=./src/test/resources
- Swagger UI 
	- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
	- check api-templating-controller
- REST API Generate
	- POST http://localhost:8080/api/templates?template=sample.docx
	- Body
``` json
{
		"order": {
			"number": "1",
			"name": "John",
			"lastname": "Doe",
			"glows": {
				"glow": [
					{
						"color": "red",
						"size": "XL"
					},
					{
						"color": "yellow",
						"size": "S"
					}
				]
			}
		}
}
```
- REST API Download generated file
	-  GET http://localhost:8080/api/templates/file/ee1db92f-54de-4494-9528-be762c97ec65/sample.docx
	- ee1db92f-54de-4494-9528-be762c97ec65/sample.docx is a response header "location"


- [Postman Collection](./PleoDox-Community.postman_collection.json)
	
- Check the generated documents
	- pleodox.storage.generatedDir=./generated-tmp (DEFAULT)
	- if this property is omited than Java System temporary folder will be used

# Run with Docker
	- create docker image locally: mvn package -P docker
	- or pull from docker hub: docker pull docker pull pleosoft/pleodox-community
	- start: docker run -p 8080:8080 pleodox/community

# Create your templates
use word with content controls (check https://www.youtube.com/watch?v=OtFhIqK0gec)

- Create your XML data sample (TestXMLNode xmlns must be PLEODOX)
``` xml
<?xml version="1.0" standalone="yes"?>
<TestXMLNode xmlns="PLEODOX">
	<order>
        <number>1234</number>
		<name>Peter</name>
		<lastname>Pan</lastname>
		<glows>
			<glow>
				<color>Red</color>
				<size>XL</size>
			</glow>			
			<glow>
				<color>White</color>
				<size>S</size>
			</glow>
           </glows>
	</order>
</TestXMLNode>
```
- Open the Developer tab in MS Word > XML Mapping Pane and add your custom XML as a Custom XML Part
	- the data values in your XML are just previewable sample data that is not used in the generated document
	- however it is useful while previewing the template

- You can also use a MS Plugin "Custom XML Part Editor" in order to change your XML on the fly within MS WORD
- Upload the template to "pleodox.storage.templatesDir"
