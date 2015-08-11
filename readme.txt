  -------------------------------------------------------------------------------------------------
                                Getting a Refresh Token From Google
 -------------------------------------------------------------------------------------------------
Before we begin to use the googleapps-connector, we will need to obtain a refresh token from google.
 You will need a google apps account so that you can obtain a client_secret.json file which will have
 the refresh token the googleapps-connector will use. For this sample we will demonstrate one way to
 get a refresh token.

 1) Verify in your google admin console that both of the following APIs are enabled
    a. Admin SDK API
    b. Enterprise License Manager API

 2) Acquire a client_secrets.json file. For this sample, we are going to use the OAuth 2.0
 Installed Application flow. You can reference the link below for more information.

    https://developers.google.com/maps-engine/documentation/oauth/installedapplication

 3) Download the google-api-services-admin-directory-rev41-java-1.19.0.zip

  a) Unzip google-api-services-admin-directory-rev41-java-1.19.0.zip
    $ unzip google-api-services-admin-directory-rev41-java-1.19.0.zip
  b) Copy the googleapps-connector-1.4.0.0.jar to the admin dir
    $ cp googleapps-connector-1.4.0.0.jar admin/
  c) cd into admin and execute the following command
    $ cd admin/
    $ java -jar googleapps-connector-1.4.0.0-SNAPSHOT.jar

    It will generate this:
    $ java -jar googleapps-connector-1.4.0.0-SNAPSHOT.jar
    Please open the following address in your browser:
      https://accounts.google.com/o/oauth2/auth?
        access_type=offline
        &approval_prompt=force
        &client_id=YOUR_CLIENT_ID
        &redirect_uri=urn:ietf:wg:oauth:2.0:oob
        &response_type=code
        &scope=https://www.googleapis.com/auth/admin.directory.group%20
        https://www.googleapis.com/auth/admin.directory.orgunit%20
        https://www.googleapis.com/auth/admin.directory.user%20
        https://www.googleapis.com/auth/apps.licensing

        Attempting to open that address in the default browser now...
        Please enter code:

    d) This will open up a browser, where you will have to accept consent. Once you accept consent you
     will be given a code that you need to paste back in the terminal where you ran that command
     from. This will result in a response that has the following:

        {
          "clientId" : "example-client-Id.apps.googleusercontent.com",
          "clientSecret" : "generated-sample-token",
          "refreshToken" : "generated-refresh-token"
        }
