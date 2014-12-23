#Recip.ie
========
I appologize if this isn't very clearly commented or the readme isn't very helpful, this is my first project on Github and I'm still learning

##What it is
Recip.ie is a Google Glass application created to allow people to be more adventurous with their cooking. The idea is to provide users with ingreident pairings they wouldn't think of on their own to experiment with. For cooks who aren't as adventurous, it can provide recipes that include their seed ingredient.

A good place to use this might be at the farmers market when you're trying to decide what to do with a new ingredeint you've just found.

##How it works

1. Start the app with the voice command "Ok Glass, Cook with this". This will only work if debugging is enabled on your device. A picture will be taken immediately.

2. This picture will be uplaoded to the CamFind api and recognized. You will then be able to select between seeing ingredients that pair well with your "seed ingredient" or recipes that include your seed ingredient
  
  a. For ingredient pairings, I used Jsoup to parse ingredientpairings.com
  
  b. For recipe listings, I used the recipe puppy api.
  
##Project Status

I don't currently have access to a Glass, so I can't really continue to actively develop this project right now.

###To-Do:
If I had some more time and access to a Glass, this is what I would want to do with this application:
- [ ] Use [Food Pairing API](https://www.foodpairing.com/en/home) instead of ingredientpairings.com html parsing
- [ ] Change the recipe engine so that I get better results
- [ ] Run some user tests

If you want to contribute, let me know.
