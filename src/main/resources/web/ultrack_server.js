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
    startServer: async function (status) {
        connection_successfull = false;

        available_configs = null

        while (!connection_successfull) {
            await new Promise(r => setTimeout(r, 1000));
            fetch('http://127.0.0.1:' + PORT + '/config/available')
                .then(response => {
                    // Check if the request was successful
                    if (response.ok) {
                        return response.json();
                    }
                })
                .then(data => {
                    available_configs = data;

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

                    document.getElementById('select-options').addEventListener('change', function() {
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
                })
        }
        showBody();
    },
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
    finishTracking: function () {
        hideLoadingOverlay();
        document.getElementById("viewButton").disabled = false;
        document.getElementById("viewButton").classList.replace("btn-outline-primary", "btn-primary")
        document.getElementById("runButton").classList.replace("btn-primary", "btn-secondary")
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
document.getElementById('runButton').addEventListener('click', function() {
    var url = document.getElementById('select-options').options[document.getElementById('select-options').selectedIndex].value;
    var updatedJson = editor.get();

    try {
        javaConnector.connectToUltrackWebsocket(url, JSON.stringify(updatedJson))
        showLoadingOverlay();
    } catch (e) {
        document.getElementById("startServer").innerHTML = e;
    }
});

document.getElementById('viewButton').addEventListener('click', function() {
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