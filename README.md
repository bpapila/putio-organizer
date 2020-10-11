# putio-organizer

Generic download folders are filled with random items. That's the case especially if you are using RSS feeds in **put.io**. 

This code attempts to organize these generic folders and move the files to a more logical location. Combined with **put.io** webhooks, this code can work whenever a download finished and in result automatically organize your library.  

Currently, it only supports TV Series format. 

TV series generally follow a folder and naming convention. They are in a folder named such as;
`{Series Name} S{season number}E{episode number} {format}}`
Inside the folder, there is a video file named generally exactly as the folder name.

After running the code, the video file will be placed such as
`TV SERIES/{Series Name}/Season {season number}/{video file}`




 
