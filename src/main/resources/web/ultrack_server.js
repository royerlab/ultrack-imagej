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

function validateAllForms() {
    'use strict'

    var allValid = true;

    // Fetch all the forms we want to apply custom Bootstrap validation styles to
    var forms = document.querySelectorAll('.needs-validation')

    // Loop over them and prevent submission
    Array.prototype.slice.call(forms)
        .forEach(function (form) {
            let valid = form.checkValidity()
            allValid = allValid && valid

            var wrongField = form.querySelector('.is-invalid')
            if (wrongField) {
                wrongField.focus()
            }
        })

    return allValid
}

(function () {
    'use strict'


    // build forms dinamically
    let forms = {
        "segmentation": {
            "title": "Segmentation",
            "fields": {
                "threshold": {
                    "label": "Threshold",
                    "type": "number",
                    "step": "any",
                    "required": true,
                    "tooltip": "Threshold value for the segmentation algorithm",
                    "validationMessage": "Please enter a valid threshold value (number)"
                },
                "min_area": {
                    "label": "Min Area",
                    "type": "number",
                    "min": 0,
                    "required": true,
                    "tooltip": "Minimum area for detected objects",
                    "validationMessage": "Please enter a valid minimum area value (number)"
                },
                "max_area": {
                    "label": "Max Area",
                    "type": "number",
                    "min": 1,
                    "required": true,
                    "tooltip": "Maximum area for detected objects",
                    "validationMessage": "Please enter a valid maximum area value (number)"
                },
                "min_frontier": {
                    "label": "Min Frontier",
                    "type": "number",
                    "step": "any",
                    "required": true,
                    "tooltip": "Minimum frontier for detected objects",
                    "validationMessage": "Please enter a valid minimum frontier value (number)"
                },
                "n_workers": {
                    "label": "Number of Workers",
                    "type": "number",
                    "min": 1,
                    "required": true,
                    "tooltip": "Number of workers to use for segmentation",
                    "validationMessage": "Please enter a valid number of workers (> 1)"
                }
            }
        },
        "linking": {
            "title": "Linking",
            "fields": {
                "distance_weight": {
                    "label": "Distance Weight",
                    "type": "number",
                    "step": "any",
                    "required": true,
                    "tooltip": "Distance weight for linking",
                    "validationMessage": "Please enter a valid distance weight value (number)"
                },
                "n_workers": {
                    "label": "Number of Workers",
                    "type": "number",
                    "min": 1,
                    "required": true,
                    "tooltip": "Number of workers to use for linking",
                    "validationMessage": "Please enter a valid number of workers (> 1)"
                },
                "max_neighbors": {
                    "label": "Max Neighbors",
                    "type": "number",
                    "min": 1,
                    "required": true,
                    "tooltip": "Maximum number of neighbors",
                    "validationMessage": "Please enter a valid maximum number of neighbors (> 1)"
                },
            }
        },
        "tracking": {
            "title": "Tracking",
            "fields": {
                "appear_weight": {
                    "label": "Appear Weight",
                    "type": "number",
                    "max": "0.0",
                    "step": "any",
                    "required": true,
                    "tooltip": "Appear weight for tracking",
                    "validationMessage": "Please enter a valid appear weight value (negative number)"
                },
                "disappear_weight": {
                    "label": "Disappear Weight",
                    "type": "number",
                    "max": "0.0",
                    "step": "any",
                    "required": true,
                    "tooltip": "Disappear weight for tracking",
                    "validationMessage": "Please enter a valid disappear weight value (negative number)"
                },
                "division_weight": {
                    "label": "Division Weight",
                    "type": "number",
                    "max": "0.0",
                    "step": "any",
                    "required": true,
                    "tooltip": "Division weight for tracking",
                    "validationMessage": "Please enter a valid division weight value (negative number)"
                },
                "window_size": {
                    "label": "Window Size",
                    "type": "number",
                    "min": 1,
                    "tooltip": "Window size for tracking",
                    "validationMessage": "Please enter a valid window size value (number or blank)"
                },
                "overlap_size": {
                    "label": "Overlap Size",
                    "type": "number",
                    "min": 1,
                    "required": true,
                    "tooltip": "Overlap size for tracking",
                    "validationMessage": "Please enter a valid overlap size value (number)"
                },
                "solution_gap": {
                    "label": "Solution Gap",
                    "type": "number",
                    "step": "any",
                    "required": true,
                    "tooltip": "Solution gap for tracking",
                    "validationMessage": "Please enter a valid solution gap value (number)"
                },
                "n_threads": {
                    "label": "Number of Threads",
                    "type": "number",
                    "min": -1,
                    "required": true,
                    "tooltip": "Number of Threads to use for tracking. -1 for all available threads.",
                    "validationMessage": "Please enter a valid number of threads"
                }
            }
        }
    }

    // generate bootstrap 5 tabs

    let formContainer = document.getElementById('form-container')
    let tabs = document.createElement('ul')
    tabs.classList.add('nav', 'nav-tabs')
    tabs.id = 'myTab'
    tabs.role = 'tablist'
    for (let key in forms) {
        let tab = document.createElement('li')
        tab.classList.add('nav-item')
        let button = document.createElement('button')
        button.classList.add('nav-link')
        button.id = key + '-tab'
        button.dataset.bsToggle = 'tab'
        button.dataset.bsTarget = '#' + key
        button.type = 'button'
        button.role = 'tab'
        button.ariaControls = key
        button.ariaSelected = key === 'segmentation'
        button.innerHTML = forms[key].title
        tab.appendChild(button)
        tabs.appendChild(tab)
    }

    formContainer.appendChild(tabs)

    let tabContent = document.createElement('div')
    tabContent.classList.add('tab-content')
    tabContent.id = 'myTabContent'

    for (let key in forms) {
        let tabPane = document.createElement('div')
        tabPane.classList.add('tab-pane', 'fade')
        tabPane.id = key
        tabPane.role = 'tabpanel'
        tabPane.ariaLabelledby = key + '-tab'
        // tabPane.hidden = key !== 'segmentation'

        let form = document.createElement('form')
        form.classList.add('needs-validation', 'was-validated')
        form.noValidate = true

        for (let field in forms[key].fields) {
            let div = document.createElement('div')
            div.classList.add('mb-3')

            let label = document.createElement('label')
            label.htmlFor = field
            label.innerHTML = forms[key].fields[field].label

            // <button type="button" class="btn btn-secondary" data-bs-toggle="tooltip" data-bs-placement="right" title="Tooltip on right">
            //   Tooltip on right
            // </button>

            let tooltip = document.createElement('button')
            tooltip.type = 'button'
            tooltip.classList.add('btn', 'btn-light', 'btn-sm', 'bi', 'bi-question-circle')
            tooltip.dataset.bsToggle = 'tooltip'
            tooltip.dataset.bsPlacement = 'right'
            tooltip.title = forms[key].fields[field].tooltip

            label.appendChild(tooltip)
            div.appendChild(label)

            let input = document.createElement('input')
            input.classList.add('form-control', 'control-sm')
            input.id = key + "_" + field
            input.name = field
            input.type = forms[key].fields[field].type
            input.step = forms[key].fields[field].step || 1
            input.min = forms[key].fields[field].min || ''
            input.max = forms[key].fields[field].max || ''
            input.required = forms[key].fields[field].required || false
            div.appendChild(input)

            let invalidFeedback = document.createElement('div')
            invalidFeedback.classList.add('invalid-feedback')
            invalidFeedback.innerHTML = forms[key].fields[field].validationMessage
            div.appendChild(invalidFeedback)

            form.appendChild(div)
        }

        tabPane.appendChild(form)
        tabContent.appendChild(tabPane)
    }

    formContainer.appendChild(tabContent)


})()

function updateImageFields(json) {
    let formImages = document.getElementById('form-images')
    formImages.innerHTML = ''
    const fields = {
        "image_channel_or_path": "Image",
        "edges_channel_or_path": "Edges Image",
        "detection_channel_or_path": "Detection Image",
        "labels_channel_or_path": "Labels Image"
    }

    for (const [field, description] of Object.entries(fields)) {
        // check if field is in json
        if (field in json && json[field] != null) {
            let div = document.createElement('div')
            div.classList.add('mb-3')

            let label = document.createElement('label')
            label.htmlFor = field
            label.innerHTML = description

            let input = document.createElement('input')
            input.classList.add('form-control', 'control-sm')
            input.id = field
            input.name = field
            input.type = 'text'
            input.value = json[field]
            input.readOnly = true
            div.appendChild(label)
            div.appendChild(input)

            formImages.appendChild(div)
            _json.experiment[field] = json[field]
        }
    }

}

function updateFormWithJson(json) {
    for (let key in json) {
        let form = document.getElementById(key)
        if (!form) {
            continue
        }
        for (let field in json[key]) {
            let input = form.querySelector('#' + key + "_" + field)
            if (input) {
                input.value = json[key][field]
            }
        }
    }

}

function updateJsonWithForm(json) {
    for (let key in json.experiment.config) {
        let form = document.getElementById(key)
        if (!form) {
            continue
        }
        for (let field in json.experiment.config[key]) {
            let input = form.querySelector('#' + key + "_" + field)
            if (input) {
                if (input.value === "") {
                    json.experiment.config[key][field] = null
                } else {
                    json.experiment.config[key][field] = input.value
                }
            }
        }
    }
    return json
}

var _json

function get_json() {
    json = _json
    json = updateJsonWithForm(json)
    return json
}

function set_json(json) {
    _json = json
    updateFormWithJson(json.experiment["config"])
    updateImageFields(json.experiment)
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
        prev = get_json()
        prev.experiment = JSON.parse(json)
        set_json(prev)
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
                    set_json(JSON.parse(json))

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
            json = get_json()
            images_json = JSON.parse(images)
            for (var i = 0; i < images_json.length; i++) {
                json.experiment[images_json[i][0]] = images_json[i][1]
            }
            set_json(json)
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

document.getElementById("selectImages").addEventListener("click", function () {
    if (validateAllForms()) {
        json = get_json();
        image_options = ["image_channel_or_path", "edges_channel_or_path",
            "detection_channel_or_path", "labels_channel_or_path"]

        available = []
        for (var i = 0; i < image_options.length; i++) {
            if (image_options[i] in json.experiment && json.experiment[image_options[i]] != null) {
                available.push(image_options[i])
            }
        }
        javaConnector.requestImages(JSON.stringify(available))
    }
})

// Function for run button
document.getElementById('runButton').addEventListener('click', function () {
    var url = document.getElementById('select-options').options[document.getElementById('select-options').selectedIndex].value;
    var updatedJson = get_json();

    try {
        javaConnector.connectToUltrackWebsocket(url, JSON.stringify(updatedJson))
        showLoadingOverlay();
    } catch (e) {
        document.getElementById("startServer").innerHTML = e;
    }
});

document.getElementById('viewButton').addEventListener('click', function () {
    experimentJson = get_json();
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
