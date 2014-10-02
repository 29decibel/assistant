## Assitant
A super simple, extensible and powerful personal assitant, just like your shell, with the power of HTML.

![short-intro](https://asistant-assets.s3.amazonaws.com/short-intro-video.gif)

## What is that?
Assistant is more like a [hubot](https://github.com/github/hubot) with rich HTML interface([Om](https://github.com/swannodette/om) component), or like a Siri on your desktop.
It consists of multiple dispatchers(processors) and cards.
Dispatcher(processor) process the text commands, then put the result into the result channel.
Assistant will use the correspond card(a [Om](https://github.com/swannodette/om) component) to show the data.

### Here is a simple diagram of how it works
![assistant-structure](https://asistant-assets.s3.amazonaws.com/assistant-structure.jpg)

## Build-in cards

### image-list-card
  ![image-list-card-hand-draw](https://s3.amazonaws.com/asistant-assets/assistant-list-image-card.png)

  * Data Structure:

  ```clojure
  { :card :image-list-card
    :content [
      {:url "http://example-url" :image "http://some-image-url/a.jpg" :title "Item title"}
      ...
    ]
  }
  ```

### list-card
  ![list-card-hand-draw](https://s3.amazonaws.com/asistant-assets/assistant-list-card.png)

  * Data Structure:

  ```clojure
  { :card :list-card
    :content [
      {:url "http://example-url" :title "Item title"}
      ...
    ]
  }
  ```

### markdown-card

  * Data Structure:

  ```clojure
  { :card :markdown-card
    :content "## Markdown title \n Content area... \n ![image](http://someawesome-image.jpg)"
  }
  ```

## Write your plugin
1. Write a processor

  A processor is just a function take a result channel and the text user typed in, then do what ever you want, at last
  put the result into the chanel.

  In this case, we make a request to Jira api, then put the JSON response into the channel and tell assistant to use jira card to
  render the result.

  ```clojure
  (defn jira-dispatcher [result-chan text]
    (go (let [response (<! (http/get (str endpoint "/rest/api/2/issue/" text) {:basic-auth {:username username :password password}}))
              m (:body response)
              error (:errorMessages m)
              issue-key (:key m)]
          (when issue-key
            (>! result-chan {:type :jira :content m :input text})))))
  ```

2. Write a card

  You don't have to write a card to show the result of your stuff, we have some built in cards.
  But if those cards not what you want, then you can always easily wrap your own.
  Here is a simple card(Om component) of for Jira issue.

  ```clojure
  (defn jira-view [data owner]
    (reify
      om/IRender
      (render [_]
              (dom/div #js {:className "clearfix"}
                       (common/link (str endpoint "/browse/" (:input data)) (dom/h4 nil (-> data :content :key) "  " (-> data :content :fields :summary)))
                       (dom/p #js {:dangerouslySetInnerHTML #js {:__html (clojure.string/replace (or
  ```

3. Register

  * Register processor
    ```clojure
    (register-card :jira jira-view)
    ```

  * Register card
    ```clojure
    (register-dispatcher :jira jira-dispatcher "jira [issue-number] -- find jira issue")
    ```

  * Register CSS(optional)
    If you want to provide customized CSS to your component, you can do that through ```register-css```:
    ```clojure
    (register-css [:.jenkins-card
                   [:code {:max-height "30px" :height "30px" :overflow "auto"}]
                   [:a {:display "inline-block" :min-width "200px"}]])
    ```

  * Register plugin namespace
    Go to ```public/index.html``` page, register your module namespace. Put something like this under `services`:
    ```javascript
    goog.require("your_plugin.namespace");
    ```

4. Build

  ```bash
  lein cljsbuild auto
  ```

5. Build standalone app

  ```bash
  lein node-webkit-build
  ```

## TODO
* markdown-card
* Pipes (dispatcher can pass result/clojure data structure to next dispatcher)
* Storage service support
* Check required parameters
* System services
* ~~Generic cards(List Card, Image Card, Readable Article List Card, JSON Viewer Card)~~
* ~~Auto save app state~~
* ~~Command line like style~~
* ~~Create dot file if not exists~~

## More cards
* Weather -- https://github.com/erikflowers/weather-icons
* Movie Showtime -- https://developer.fandango.com/io-docs
* Hotel price search -- http://developer.ean.com/docs/common/
* Flight number look up
* Fantastical like Calendar
* Flight price search
* ~~Dribble popular designs~~
* ~~Jenkins~~


## Inspired by
* [github/hubot](https://github.com/github/hubot)
* [bhauman/devcards](https://github.com/bhauman/devcards)
* [Her](http://www.imdb.com/title/tt1798709/)

## Credits goes to
* [clojure/clojure](https://github.com/clojure/clojure)
* [facebook/react](https://github.com/facebook/react)
* [swannodette/om](https://github.com/swannodette/om)
* [noprompt/garden](https://github.com/noprompt/garden)
* [rogerwang/node-webkit](https://github.com/rogerwang/node-webkit)
* [clojure/core.async](https://github.com/clojure/core.async)
* [cognitect/transit-cljs](https://github.com/cognitect/transit-cljs)

__Build with ♥ Clojure+Clojurescript.__
