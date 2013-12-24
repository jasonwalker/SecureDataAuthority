#Secure Data Authority
An encrypted email-like system for small groups to communicate securely.  

-------------------------

* Easy setup and use.
* Send messages with strong encryption.
* Data encrypted on client ends, server cannot decrypt mail
* Lightweight--the server can run on small devices such as the Raspberry Pi in addition to regular desktop computers.  

# Quick Start
## Server Side
 * required Java 1.7 or higher.
 * Start server--stand-alone method

    Download [SDAServer-0.1.0-jetty-console.war](https://github.com/jasonwalker/SecureDataAuthority/releases/download/0.1.0/SDAServer-0.1.0-jetty-console.war) and run:
    ```sh
    java -jar SDAServer-0.1.0-jetty-console.war
    ```
    On the window that pops up, choose a desired port number and click the "Start" button.  
    Wait while the RSA Key pairs are being generated for first run.
  
 * Start server--servlet engine method

    Download [SDAServer-0.1.0.war](https://github.com/jasonwalker/SecureDataAuthority/releases/download/0.1.0/SDAServer-0.1.0.war) and place it in the servlet engine's WAR directory.

 	
 	_NOTE: The RSA strength defaults to 3072 bits.  If you would like to change that, create a file in the servlet root directory
 	(the directory where the jar file is located if running standalone) named "strength" and add the line "strength=7680" for 
 	7680-bit RSA encryption or a number specifying whatever strength you prefer._

-----
## Client Side

Create a mailbox

  * Go to web page being served, most likely http://localhost:8080 and click on "Secure Data Client" which will download
  a file named "SDAClient.jar".
  * Double click "SDAClient.jar".  If that doesn't open a GUI, open a command prompt and type:
```
  java -jar SDAClient.jar
```
  * Go to "Create Mailbox" tab.  
  * Underneath "Please supply the server's URL", type in the URL, most likely http://localhost:8080
  * Back on the server's web page, click on "Public Key", copy the resulting text into the text area underneath 
  the text: "Please paste the server's plubic key in the space below.  It is very important that this key is correct."
  * Underneath "Please supply a mailbox name" enter the name you would like.  It can be any alpha numeric string.
  * Click the "Create Mailbox" button.
  * Wait for Public/Private key pairs to be generated and mailbox to be registered on the server.
 
Send mail (very similar to email)

  * Go to "Mailbox" tab
  * Click "New" button
  * Select "To" mailboxes from dropdown.
  * To refresh "To" mailboxes, click "To" button
  * Type subject and note
  * If any attachments, click "Attachment" and select files
  * Press "Send Message"

Receive mail (very similar to email)

  * On "Mailbox" tab, click "Refresh" button
  * Either double click message from list or highlight message in list and click "Read" button
  
# FAQ

## What problem does this solve:

Creates an easy to use encryption solution by making acceptable choices about encryption to use along with simple 
server initialization and client setup.  (For example, your mom wants to send something to her CPA.  Neither of
them know anything about SFTP, GPG, etc.  You can set up a server and they both create accounts and send data
back and forth)

## What are those acceptable choices?

 * 3072-bit RSA with 256-bit AES session key  
 * Separate RSA key pairs for encryption and signing
 * Central server publishes client-generated public keys

## Why did you chose 3072 RSA and 256 AES for default encryption strength?

256-bit AES was chosen because it is very strong and the time to generate the AES key is inconsequential.  The
weaker point is the RSA key.  3072-bit RSA is an equivalent strength to 128-bit AES.  The most time-consuming part is of 
this is generating the initial RSA public/private key pair that is associated with the 
mailbox--3072 bits is a good balance between security and speed.  Note that the configuration can be changed to increase
the bit-strength.  A strength of 15360-bit RSA is equivalent to 256-bit AES.  On my desktop computer, the time 
needed to generate the 15360-bit keys averaged around an hour.

## Why separate RSA key pairs for encryption and signing?

[There is a lot written about this.](http://stackexchange.com/search?q=separate+signing+and+encrypting+key) In short, 
it can protect against certain kinds of attacks.

## Don't I need to add some file to my Java installation to use encryption this strong?

No.  Normally you would have to download the "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files" 
but I worked around it by directly using Bouncy Castle's APIs instead of Bouncy Castle's cryptography provider.

## Have you implemented Forward Secrecy?

No.  The quick reason is if Forward Secrecy comes into play it means that either the private key of the recipient has been compromised
either by 1) the attacker computationally breaking the key or 2) the attacker gaining access to the recipient's computer.
If #1, then the attacker can probably also computationally break a Diffie-Hellman-derived secret key.  If #2, then the
attacker probably has access to all of your stuff anyways.  Also not using Diffie-Hellman simplifies the system operation and 
allowed me to increase the efficiency of forwarding attachments.  (However, I'm still toying with the idea)  

##  What are some potential weaknesses in your approach?

 * In using Java, we really have to trust that Java's SecureRandom implementation is a good pseudo random number generator.

 * We have to believe that the RSA and AES standards have not been deliberately weakened by a standards-influencing 
 body--although one could fairly easily add code to substitute in different encryption algorithms.

 * The private key pairs are sitting on your computer, unsecured.  (One way to mitigate this risk would be to keep 
 the SDAClient.jar on a USB drive and run it directly off of that.)  The server must be secured.

 * We have to trust that the server is giving us the correct public key for the designated recipients.  The
risk is mitigated in that you, or someone you trust, should be the one running the server.

# Building
  * install maven -- http://maven.apache.org/download.cgi
  
  * cd into SecureDataAuthority directory and type:
```sh
  mvn install
```
  Server executable output goes to the directory SDAServer/target.  Two files are created:
  
     1.  SDAServer-0.1.0-jetty-console.war for running WAR file directly (see Quickstart)
     2.  SDAServer-0.1.0.war for running in a servlet engine
  
  * To perform test run change to directory "SDAServer" and type:
```sh
  mvn -DskipTests jetty:run-war
```

# Future Work

 * Currently all of the data is stored on the server's filesystem.  A module that stores the data (remember it cannot be
decrypted by the server) on a cloud-based storage provider, such as Amazon Web Services

 * Different cryptographic algorithms besides RSA and AES could be plugged in (they must implement an interface)
