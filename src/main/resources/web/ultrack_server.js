/*-
 * #%L
 * Ultrack: Large-Scale Multi-Hypotheses Cell Tracking Using Ultrametric Contours Maps.
 * %%
 * Copyright (C) 2010 - 2024 RoyerLab.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
let PORT = 8000;

function showLoadingOverlay() {
    document.querySelector('.loading-overlay').style.display = 'flex';
}

function hideLoadingOverlay() {
    document.querySelector('.loading-overlay').style.display = 'none';
}


function startServerFn() {
    showLoadingOverlay();
    javaConnector.startUltrackServer()
    document.getElementById('startServer').classList.remove('btn-success');
    document.getElementById('startServer').classList.add('btn-danger');
    document.getElementById('startServer').innerHTML = 'Stop Ultrack Server';
    // document.getElementById('server-alert').hidden = false;
    document.getElementById('startServer').onclick = stopServerFn;
}

function stopServerFn() {
    showLoadingOverlay();
    javaConnector.stopUltrackServer()
    document.getElementById('startServer').classList.remove('btn-danger');
    document.getElementById('startServer').classList.add('btn-success');
    document.getElementById('startServer').innerHTML = 'Start Ultrack Server';
    // document.getElementById('server-alert').hidden = true;
    document.getElementById('startServer').onclick = startServerFn;
    hideBody();
}

function hideBody() {
    document.getElementById('jsoneditor-div').hidden = true;
}

function showBody() {
    document.getElementById('jsoneditor-div').hidden = false;
}

function getJsConnector() {
    return jsConnector;
}

var jsConnector = {
    showResult: function (result) {
        document.getElementById('result').innerHTML = result;
    },
    setPort: function (port) {
        PORT = port;
    },
    successfullyStopped: function () {
        hideLoadingOverlay();
    },
    updateJson: function (json) {
        prev = editor.get()
        prev.experiment = JSON.parse(json)
        editor.set(prev)
    },
    startServer: function (status) {
        function fetchConfigs() {
            w = new Worker("connect_server.js");
            w.onmessage = function (event) {
                available_configs = event.data;

                hideLoadingOverlay();
                document.getElementById('select-options').innerHTML = '';

                Object.keys(available_configs).forEach(key => {
                    link = available_configs[key]['link']
                    config = available_configs[key]['config']
                    human_name = available_configs[key]['human_name']
                    var option = document.createElement("option");
                    option.text = human_name;
                    option.value = link;
                    option.setAttribute('data-json', JSON.stringify(config));
                    document.getElementById('select-options').add(option);
                });

                document.getElementById('select-options').addEventListener('change', function () {
                    var selectedOption = this.options[this.selectedIndex];
                    var json = selectedOption.getAttribute('data-json');
                    editor.set(JSON.parse(json));

                    document.getElementById("runButton").disabled = true;
                    document.getElementById("runButton").classList.remove("btn-primary", "btn-secondary")
                    document.getElementById("runButton").classList.add("btn-outline-primary")

                    document.getElementById("selectImages").classList.remove("btn-secondary")
                    document.getElementById("selectImages").classList.add("btn-primary")
                    document.getElementById("selectImages").disabled = false;

                    document.getElementById("viewButton").disabled = true;
                    document.getElementById("viewButton").classList.remove("btn-primary")
                    document.getElementById("viewButton").classList.add("btn-outline-primary")
                });

                document.getElementById("select-options").dispatchEvent(new Event('change'));

                connection_successfull = true;
                w.terminate();
                showBody();
            };
            w.onerror = function (event) {
                // ignored
            }
            w.postMessage(PORT);
        }

        fetchConfigs();

    },
    /**
     * Update selected images.
     * @param images - A list of paths to the selected images.
     */
    updateSelectedImages: function (images) {
        try {
            json = editor.get();
            images_json = JSON.parse(images)
            for (var i = 0; i < images_json.length; i++) {
                json.experiment[images_json[i][0]] = images_json[i][1]
            }
            editor.set(json)
            document.getElementById("runButton").disabled = false;
            document.getElementById("runButton").classList.replace("btn-outline-primary", "btn-primary")
            document.getElementById("selectImages").classList.replace("btn-primary", "btn-secondary")
            document.getElementById("selectImages").disabled = true;
        } catch (e) {
        }
    },
    /**
     * Finish tracking.
     *
     * This function is called when the tracking process is finished.
     */
    finishTracking: function () {
        hideLoadingOverlay();
        document.getElementById("viewButton").disabled = false;
        document.getElementById("viewButton").classList.replace("btn-outline-primary", "btn-primary")
        document.getElementById("runButton").classList.replace("btn-primary", "btn-secondary")
    },
    /**
     * Close the connection.
     * This function is called when the user closes the websocket connection.
     */
    closeConnection: function () {
        hideLoadingOverlay();
    },
    /**
     *
     */
    resetPage: function () {
        hideLoadingOverlay();
        stopServerFn();
    }
};

// Initialize JSONEditor
var container = document.getElementById("jsoneditor");
var options = {};
var editor = new JSONEditor(container, options);

document.getElementById("selectImages").addEventListener("click", function () {
    json = editor.get();
    image_options = ["image_channel_or_path", "edges_channel_or_path",
        "detection_channel_or_path", "labels_channel_or_path"]

    available = []
    for (var i = 0; i < image_options.length; i++) {
        if (image_options[i] in json.experiment && json.experiment[image_options[i]] != null) {
            available.push(image_options[i])
        }
    }
    javaConnector.requestImages(JSON.stringify(available))
})

// Function for run button
document.getElementById('runButton').addEventListener('click', function () {
    var url = document.getElementById('select-options').options[document.getElementById('select-options').selectedIndex].value;
    var updatedJson = editor.get();

    try {
        javaConnector.connectToUltrackWebsocket(url, JSON.stringify(updatedJson))
        showLoadingOverlay();
    } catch (e) {
        document.getElementById("startServer").innerHTML = e;
    }
});

document.getElementById('viewButton').addEventListener('click', function () {
    experimentJson = editor.get();
    id = experimentJson["experiment"]["id"]

    fetch('http://127.0.0.1:' + PORT + '/experiment/' + id + '/trackmate')
        .then(response => {
            // Check if the request was successful
            if (response.ok) {
                return response.json();
            }
        })
        .then(data => {
            xml = data['trackmate_xml']
            javaConnector.viewTracks(xml)
        })
});
