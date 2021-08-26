Box Integration with Spring Boot Example
============

The Box integration with Spring Boot 

1. To Call Box API using client_credentials Token

2. Listen Box events using WebHooks



Setup on Box.com
----------
1. Login to Box.com [Developer console](https://app.box.com/developers/console) and click on My Apps on left side navigation 
2. Create New App  of type 'Custom App' with Authentication Method "Server Authentication (Client Credentials Grant)", provide name of your choice 
![Create new client_credentials app](./images/1-createApp.PNG?raw-true "Create App")
3. From Configuration Tab of created app, make a note of client id and secret, we will need it in Spring Boot configuration (you might need to setup 2-step verification to fetch client secret, it can be setup from Developer [Account Settings](https://app.box.com/account/developer) )   
4. From App Access Level, select "App + Enterprise Access" 
5. From Application scopes, select  
    * Content Actions > "Write all files and folders stored in Box", we will need it to create files and folders via api 
    * Developer Actions > Manage Webhooks - this will be needed send events from Box.com to Spring Boot API
![Application Scope](./images/2-AppScope.PNG?raw-true "Application Scope")
6. On Webhooks tab, 
    * Generate primary key and secondary key - This will be required to check authenticity of Webhook events received on SpringBoot REST service    
7. Create folder using api and share that folder with in your company so that actual user can add/delete files from that folder 	 
    * Get Access Token - Post client_id, client_secret, grant_type, box_subject_id, box_subject_type to https://api.box.com/oauth2/token. You can find enterpise id from [Box.com Billing and Account page](https://app.box.com/master/settings/accountBilling), use that as box_subject_id. I have used PostMan for all this setup.     
![Access Token](./images/Access-Token.PNG?raw-true "Get Access Token")  
    * Create folder - Post following payload to https://api.box.com/2.0/folders/ with Authorization header as Bearer <AccessToken> from earlier call. If its successful note folder id, we will need it on following call     
![Create Folder](./images/Create-Folder.PNG?raw-true "Create Folder")     
    * Check folder items to verify folder created successfully - Invoke GET call to https://api.box.com/2.0/folders/<folderId> , you should see details of folder if its created successfully 
8. Login to Admin Console 
    * Goto [Content Menu](https://app.box.com/master/content/)
    * Expand your application and select the folder that you have just created on above step
    * From Collaborator Section, select "Invite People and provide their details and permission as Editor"
    * Invitee should be able to see this folder and should be able to upload files into it now 
    
Setup on Cloned Code
----------
    
- Update com.bkakadiya.example.boxpoc.controller.BoxController with primarykey and secondoryKey that you have got from above step 6 of Box.com setup  
    
```java    
	/**
	 * TODO: For production code, Get it from Secure place
	 */
	private final String primaryKey = "aaaaaa";  // primary key from box.com webhook setup
	private final String secondaryKey = "bbbbb"; // secondary key from box.com webhook setup 
  
```
- Update com.bkakadiya.example.boxpoc.controller.MyApplicationController.java to have correct folder id that you have created via api

```java
	//TODO: UPDATE FOLDER ID HERE - BETTER TO GET IT FROM TEMPLATE/DB 
	private static final String BOX_FILE_UPLOAD_PAYLOAD = "{\"name\":\"___FILENAME__\", \"parent\":{\"id\":\"<FolderId>\"}}";
	//update <FolderId> on above line
```
- Update com.bkakadiya.example.boxpoc.util.BoxUtility with your enterprise id and your application's client_id and secret,  this will be same as we used on step 7 of box.com setup 

```java

		//TODO: GET CLIENT ID, SECRET, ENTERPRISE ID FROM Secure Vault based on user id 
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("client_id","<your_app_client_id>");
		map.add("client_secret","<your_app_client_secret>");
		map.add("grant_type","client_credentials");
		map.add("box_subject_type","enterprise");
		map.add("box_subject_id","<your_enterprise_id>");
```
 
- Run Spring Boot app BoxPocApplication  
- You should be able to see swagger document page on [localhost](http://localhost:8080/swagger-ui.html)  
- Install [NGRok](https://ngrok.com) if you not have not installed and expose port 8080. Box.com needs public facing url to post the events

```java
ngrok http 8080 

```
- Note that https url and update it in to step 6 of box.com setup of above section 


Adding Webhooks on Box.com to invoke REST api exposed by Spring Boot
----------

- Use Access token from step 7 of Box.com setup and hit web hook creation api https://api.box.com/2.0/webhooks using below sample payload (update your folder id)

```json
{
  "target": {
    "id": "<your folder id>",
    "type": "folder"
  },
  "address": "https://<ngrok https address >/api/v1/doc-requests",
  "triggers": [
    "FILE.UPLOADED",
    "FILE.TRASHED",
    "FILE.DELETED",
    "FILE.RESTORED",
    "FILE.COPIED",
    "FILE.MOVED",
    "FILE.RENAMED"    
  ]
}
```

- Http Response should be 201 for successful webhook creation 

- At this stage we have all required steps in place to test the integration    

Calling Box API from Spring Boot
----------
- Goto Swagger document page and expand my-application-controller POST endpoint
- Try it with any source name and sample file 

![Swagger File upload](./images/swagger-file-upload.PNG?raw-true "Swagger")

- If you see response code as 201, file should have been uploaded on your server (check under files folder) and should have replicated on box.com shared folder

Sending file from Box.com to Spring Boot
----------

- Goto shared folder via web console and upload a file
- This will invoke /api/v1/doc-requests endpoint on our server with trigger event details
- Sample code fetches fildId from event and then hits box.com to get actual file contents
- If everything is fine, you should be able to see that file under "files" folder of application 

To be done
----------
- When file is uploaded via our REST service and replicated on box.com, webhook is initiating event, as of now code is checking only file name but we need a better check to ensure that this is not a round trip request 
- Box.com authentication related information needs to be fetched from secure vault, as of now its hard coded in app 
