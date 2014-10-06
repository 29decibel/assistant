## Assistant
A super simple, extensible and powerful personal assistant, just like your shell, with the power of HTML.

![short-intro](https://asistant-assets.s3.amazonaws.com/short-intro-video.gif)

## What is that?
Assistant is more like a [hubot](https://github.com/github/hubot) with rich HTML interface([Om](https://github.com/swannodette/om) component), or like a Siri on your desktop.
It consists of multiple dispatchers(processors) and cards.
Dispatcher(processor) process the text commands, then put the result into the result channel.
Assistant will use the correspond card(a [Om](https://github.com/swannodette/om) component) to show the data.

## How to use?
Make sure you have [Leiningen](http://leiningen.org/) installed, if not, you can use `brew install leiningen` to install.

> WARN:
  1. Someone has issues with the older version of `leiningen`, so please upgrade it to `2.5.3`(or at least `>=2.4.3`) first if you are using an older version of `leiningen`.
  2. Many of services needs api key to work, since they are making requests to those services. Now most of the services will complain about no aip keys found in `~/.assistant`, luckly it will also tell you where to get one, for free :-)


```bash
git clone git@github.com:29decibel/assistant.git && cd assistant
lein cljsbuild once && lein node-webkit-build
```

After that, you will find a stand alone app in the `release` folder. Right now it only build the app for Mac. Will support Linux and Windows later on.
Make sure you have proper configuration in your `~/.assistant` for specific commands, you can find a example config file [here](dot_assistant_example).

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

### info-card
  Display `error`, `warn`, `info` or `success` information card to the user.

  * Data Structure:

  ```clojure
  { :card :info-card
    :content "Your config not correct, please add :jenkins :username in your ~/.assistant"}
    :info-type "error" }
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
    (register-dispatcher :jira jira-dispatcher "jira [issue-number] -- find jira issue")
    ```

  * Register card
    ```clojure
    (register-card :jira jira-view)
    ```

  * Register CSS(optional)
    If you want to provide customized CSS to your component, you can do that through ```register-css```:
    ```clojure
    (register-css [:.jenkins-card
                   [:code {:max-height "30px" :height "30px" :overflow "auto"}]
                   [:a {:display "inline-block" :min-width "200px"}]])
    ```

  * Register plugin namespace
    Add your plugin namespace name to `~/.assistant-plugins` file:
    ```
    # here is a list of your awesome plugins you want to enabled
    # WARN: If your plugin used dash, then you need to convert it to underscore, since it's javascript
    #       assistant.plugins.your-awesome-plugin --> assistant.plugins.your_awesome_plugin
    assistant.plugins.your_awesome_plugin
    ```

  Here is a source code of [example plugin for looking up Clojure doc](src/assistant/services/clojure-doc.cljs).

4. Build

  ```bash
  lein cljsbuild clean
  lein cljsbuild once
  ```

5. Build standalone app

  ```bash
  lein node-webkit-build
  ```

## Core APIs

  1. `assistant.core/register-dispatcher`
    > Register your dispatcher/processor.

    ```clojure
    (register-dispatcher :dribble dribbble-dispatcher "dribble -- show popular designs from dribbble")
    ```

  2. `assistant.core/register-card`
    > Register your custom card.

    ```clojure
    (register-card :jira jira-card)
    ```

  3. `assistant.core/register-css`
    > Using [noprompt/garden](https://github.com/noprompt/garden) to register your custom css for your card.

    ```clojure
    (register-css [:.map
                   [:iframe {:width "100%" :height "400px" :border "none"}]
                   [:img {:margin-top "2px auto"}]])
    ```

  4. `assistant.core/valid-config`
    > Check if given config exists. If not then show a error card.

    ```clojure
    ;; Here is a example how to check config informations
    (defn check-jenkins-config []
      (and (valid-config [:jenkins :host] "Please make sure following config exists in your ~/.assistant: :jenkins {:host}")
           (valid-config [:jenkins :token] "Please make sure following config exists in your ~/.assistant: :jenkins {:token }")
           (valid-config [:jenkins :username] "Please make sure following config exists in your ~/.assistant: :jenkins {:username}")
           (valid-config [:jenkins :password] "Please make sure following config exists in your ~/.assistant: :jenkins {:password}")))
    ```

## TODO
* Add a general card for errors(configuration checking)
* Add `clear` command to clear all cards
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

## FAQ
1. Why this exists?
  I am not trying to build a new shell, or revolutionize shell interface. I am also not trying to build a Alfred alternative. Since, I think, it's all start from different places to solve
  different problems. Shell is great, lightweight and highly composable(due to the abstraction of texts) through pipes. Alfred is more like a luncher, though nowadays it
  includes quite flexible work-flows, but it's interface still constrained by lists.

  The idea behind Assistant is try to use minimal concepts, elements(processor + card) to solve problems, but also at the same time make it highly
  extensible. I am trying to build a simple mind model `text --> process --> result(JSON/map/array) -> card`. Before [ReactJS](https://github.com/29decibel/assistant) exists, card parts is always become a mess.
  Either it will be very hard to control the modularity or it has too much hassle or mentally feel complicated. But since ReactJS's component is just like a card.
  Itself accepts data, then return virtual DOM elements, that's it, no more, no less. Finally I feel relieved. :-)

  Om push it into a higher level, so you can write even less code, avoid struggling another new JSX format. Have to say writing UI in Om is the first time I feel fun.


2. Why there are two places for plugins? Where should I put my plugins?
  The initial idea is to have a bunch of built in services, they are all lived in [src/assistant/services](src/assistant/services). It's also part of the git repo.
  But if you have any personal plugins want to use, you can create `cljs` file in [plugins](plugins) folder. Leinbuild will also pick it up.

  Later on I plan to use some similar plugin installation system like [Lighttable](https://github.com/LightTable/LightTable). But it needs some time.


3. Why it shows a blank card?
  Sorry for that if you ever saw it, that's probably because you don't have certain API keys for some services. So the result is empty, also if the card doesn't check the result
  properly it might render a empty card. I've added a new function `valid-config` and a built in `info-card`. So before the processor runs, it will check if the config exists, if not, it will
  put a error result into the channel, then the `info-card` will render and show the message.

  I will try to make this config check process much simpler later on.
  Also I am very open to ideas on how to improve this tool to make it more useful.


## Inspired by
* [github/hubot](https://github.com/github/hubot)
* [bhauman/devcards](https://github.com/bhauman/devcards)
* [Her](http://www.imdb.com/title/tt1798709/)

## Credits goes to
* [clojure/clojure](https://github.com/clojure/clojure)
* [clojure/clojurescript](https://github.com/clojure/clojurescript)
* [facebook/react](https://github.com/facebook/react)
* [swannodette/om](https://github.com/swannodette/om)
* [rogerwang/node-webkit](https://github.com/rogerwang/node-webkit)
* [clojure/core.async](https://github.com/clojure/core.async)
* [cognitect/transit-cljs](https://github.com/cognitect/transit-cljs)
* [noprompt/garden](https://github.com/noprompt/garden)

__Build with ♥ Clojure+Clojurescript.__
