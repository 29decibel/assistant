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

> WARN: Someone has issues with the older version of `leiningen`, so please upgrade it to `2.5.3`(or at least `>=2.4.3`) first if you are using an older version of `leiningen`.

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
