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


    let additional_options = {
        "label_to_edges_kwargs": {
            "subform": true,
            "title": "Label to Edges",
            "fields": {
                "sigma": {
                    "label": "Sigma",
                    "type": "number",
                    "min": 0.0,
                    "step": "any",
                    "tooltip": "Edges smoothing parameter (gaussian blur), edges aren't smoothed if not provided",
                    "validationMessage": "Please enter a valid sigma value (number or blank)"
                }
            }
        },
        "detect_foreground_kwargs": {
            "subform": true,
            "title": "Detect Foreground",
            "fields": {
                "sigma": {
                    "label": "Sigma",
                    "type": "number",
                    "default": 15.0,
                    "min": 0.0,
                    "step": "any",
                    "required": true,
                    "tooltip": "Sigma used to estimate background, it will be divided by voxel size",
                    "validationMessage": "Please enter a valid sigma value (number)"
                },
                "remove_hist_mode": {
                    "label": "Remove Histogram Mode",
                    "type": "checkbox",
                    "default": "false",
                    "required": false,
                    "tooltip": "Removes histogram mode before computing otsu threshold, useful when background regions are being detected",
                    "validationMessage": "Please enter a valid remove histogram mode value (boolean)"
                },
                "min_foreground": {
                    "label": "Min Foreground",
                    "type": "number",
                    "min": 0.0,
                    "step": "any",
                    "default": 0.0,
                    "required": true,
                    "tooltip": "Minimum value of foreground pixels after background subtraction and smoothing",
                    "validationMessage": "Please enter a valid min foreground value (number)"
                },
                "channel_axis": {
                    "label": "Channel Axis",
                    "type": "number",
                    "min": 0,
                    "max": 4,
                    "required": false,
                    "tooltip": "When provided it will be used to compute the foreground mask for each channel separately and merge them",
                    "validationMessage": "Please enter a valid channel axis value (number or blank)"

                }
            }
        },
        "robust_invert_kwargs": {
            "subform": true,
            "title": "Robust Invert",
            "fields": {
                "sigma": {
                    "label": "Sigma",
                    "type": "number",
                    "min": 0.0,
                    "default": 1.0,
                    "required": true,
                    "step": "any",
                    "tooltip": "Sigma used to smooth the image",
                    "validationMessage": "Please enter a valid sigma value (number)"
                },
                "lower_quantile": {
                    "label": "Lower Quantile",
                    "type": "number",
                    "max": 1.0,
                    "min": 0.0,
                    "step": "any",
                    "required": false,
                    "tooltip": "Lower quantile used to clip the intensities, minimum used when None",
                    "validationMessage": "Please enter a valid lower quantile value (number or blank)"
                },
                "upper_quantile": {
                    "label": "Upper Quantile",
                    "type": "number",
                    "max": 1.0,
                    "min": 0.0,
                    "step": "any",
                    "required": false,
                    "tooltip": "Upper quantile used to clip the intensities, maximum used when None",
                    "validationMessage": "Please enter a valid upper quantile value (number or blank)"
                }
            }
        }
    }

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

    let extended_forms = forms
    extended_forms["additional_options"] = {
        "title": "Additional Options",
        "fields": additional_options
    }

    // generate bootstrap 5 tabs

    let formContainer = document.getElementById('form-container')
    let tabs = document.createElement('ul')
    tabs.classList.add('nav', 'nav-tabs')
    tabs.id = 'myTab'
    tabs.role = 'tablist'
    for (let key in extended_forms) {
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
        button.innerHTML = extended_forms[key].title
        tab.appendChild(button)
        tabs.appendChild(tab)
    }

    formContainer.appendChild(tabs)

    let tabContent = document.createElement('div')
    tabContent.classList.add('tab-content')
    tabContent.id = 'myTabContent'

    for (let key in forms) {
        let _key = "" + key
        let tabPane = document.createElement('div')
        tabPane.classList.add('tab-pane', 'fade')
        tabPane.id = key
        tabPane.role = 'tabpanel'
        tabPane.ariaLabelledby = key + '-tab'
        // tabPane.hidden = key !== 'segmentation'

        let form = document.createElement('form')
        form.classList.add('needs-validation', 'was-validated')
        form.noValidate = true

        let fields = forms[key].fields

        let queue_fields_to_add = fields
        let current_subform = null
        let _form = form

        while (Object.keys(queue_fields_to_add).length > 0) {
            let field_name = Object.keys(queue_fields_to_add)[0]
            let field = queue_fields_to_add[field_name]

            if (field.subform !== undefined && field.subform === true) {
                if (current_subform) {
                    form.appendChild(current_subform)
                }
                current_subform = document.createElement('div')
                key = _key + "_" + field_name
                current_subform.id = key
                _form = current_subform

                _form.classList.add('mb-3')
                let subform_title = document.createElement('h5')
                subform_title.innerHTML = field.title
                _form.appendChild(subform_title)

                queue_fields_to_add = {...field.fields, ...queue_fields_to_add}
            } else {
                let div = document.createElement('div')
                div.classList.add('mb-3')

                let label = document.createElement('label')
                label.htmlFor = field_name
                label.innerHTML = field.label

                // <button type="button" class="btn btn-secondary" data-bs-toggle="tooltip" data-bs-placement="right" title="Tooltip on right">
                //   Tooltip on right
                // </button>

                let tooltip = document.createElement('button')
                tooltip.type = 'button'
                tooltip.classList.add('btn', 'btn-light', 'btn-sm', 'bi', 'bi-question-circle')
                tooltip.dataset.bsToggle = 'tooltip'
                tooltip.dataset.bsPlacement = 'right'
                tooltip.title = field.tooltip
                label.appendChild(tooltip)

                let input = document.createElement('input')
                input.classList.add('form-control', 'control-sm', "json-input")
                input.id = key + "_" + field_name
                input.name = field_name
                input.type = field.type
                if (field.default !== undefined)
                    input.value = field.default
                input.step = field.step || 1
                if (field.min !== undefined)
                    input.min = field.min
                if (field.max !== undefined)
                    input.max = field.max
                input.required = field.required || false

                let invalidFeedback = document.createElement('div')
                invalidFeedback.classList.add('invalid-feedback')
                invalidFeedback.innerHTML = field.validationMessage

                if (field.type === 'checkbox') {
                    input.classList.remove('form-control')
                    input.classList.add('form-check-input')
                    label.classList.add('form-check-label')
                    div.appendChild(input)
                    div.appendChild(label)
                } else {
                    div.appendChild(label)
                    div.appendChild(input)
                }
                div.appendChild(invalidFeedback)

                _form.appendChild(div)
                // let div = document.createElement('div')
                // div.classList.add('mb-3')
                //
                // let label = document.createElement('label')
                // label.htmlFor = subfield
                // label.innerHTML = field.fields[subfield].label
                //
                // let tooltip = document.createElement('button')
                // tooltip.type = 'button'
                // tooltip.classList.add('btn', 'btn-light', 'btn-sm', 'bi', 'bi-question-circle')
                // tooltip.dataset.bsToggle = 'tooltip'
                // tooltip.dataset.bsPlacement = 'right'
                // tooltip.title = field.fields[subfield].tooltip
                //
                // label.appendChild(tooltip)
                // div.appendChild(label)
                //
                // let input = document.createElement('input')
                // input.classList.add('form-control', 'control-sm')
                // input.id = key + "_" + subfield
                // input.name = subfield
                // input.type = field.fields[subfield].type
                // input.step = field.fields[subfield].step || 1
                // input.min = field.fields[subfield].min || ''
                // input.max = field.fields[subfield].max || ''
                // input.required = field.fields[subfield].required || false
                // div.appendChild(input)
                //
                // let invalidFeedback = document.createElement('div')
                // invalidFeedback.classList.add('invalid-feedback')
                // invalidFeedback.innerHTML = field.fields[subfield].validationMessage
                // div.appendChild(invalidFeedback)
                //
                // _form.appendChild(div)
            }

            delete queue_fields_to_add[field_name]
        }

        if (current_subform) {
            form.appendChild(current_subform)
        }

        // for (let field in forms[key].fields) {
        //     let div = document.createElement('div')
        //     div.classList.add('mb-3')
        //
        //     let label = document.createElement('label')
        //     label.htmlFor = field
        //     label.innerHTML = forms[key].fields[field].label
        //
        //     // <button type="button" class="btn btn-secondary" data-bs-toggle="tooltip" data-bs-placement="right" title="Tooltip on right">
        //     //   Tooltip on right
        //     // </button>
        //
        //     let tooltip = document.createElement('button')
        //     tooltip.type = 'button'
        //     tooltip.classList.add('btn', 'btn-light', 'btn-sm', 'bi', 'bi-question-circle')
        //     tooltip.dataset.bsToggle = 'tooltip'
        //     tooltip.dataset.bsPlacement = 'right'
        //     tooltip.title = forms[key].fields[field].tooltip
        //
        //     label.appendChild(tooltip)
        //     div.appendChild(label)
        //
        //     let input = document.createElement('input')
        //     input.classList.add('form-control', 'control-sm')
        //     input.id = key + "_" + field
        //     input.name = field
        //     input.type = forms[key].fields[field].type
        //     input.step = forms[key].fields[field].step || 1
        //     input.min = forms[key].fields[field].min || ''
        //     input.max = forms[key].fields[field].max || ''
        //     input.required = forms[key].fields[field].required || false
        //     div.appendChild(input)
        //
        //     let invalidFeedback = document.createElement('div')
        //     invalidFeedback.classList.add('invalid-feedback')
        //     invalidFeedback.innerHTML = forms[key].fields[field].validationMessage
        //     div.appendChild(invalidFeedback)
        //
        //     form.appendChild(div)
        // }

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

        if ((json[field] === undefined || json[field] == null) && _json !== undefined &&
            _json.experiment !== undefined && _json.experiment[field] !== undefined) {
            json[field] = _json.experiment[field]
        }

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
    let additional_options = ["label_to_edges_kwargs", "detect_foreground_kwargs", "robust_invert_kwargs"]

    for (let key in additional_options) {
        key = additional_options[key]
        id = "additional_options_" + key
        let form = document.getElementById(id)
        if (!form) {
            continue
        }
        if (!json[additional_options[key]]) {
            continue
        }
        for (let field in json[key]) {
            let input = form.querySelector('#' + id + "_" + field)
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
                    json.experiment.config[key][field] = parseFloat(input.value)
                }
            }
        }
    }
    let additional_options = ["label_to_edges_kwargs", "detect_foreground_kwargs", "robust_invert_kwargs"]

    for (let key in additional_options) {
        key = additional_options[key]
        id = "additional_options_" + key
        let form = document.getElementById(id)
        if (!form) {
            continue
        }
        if (!(key in json)) {
            continue
        }
        // select all the fields in the form
        inputs = form.querySelectorAll('.json-input')


        for (let i = 0; i < inputs.length; i++) {
             input = inputs[i]
        //     //let input = form.querySelector('#' + id + "_" + field)
             if (input) {

                 if (input.name && input.value !== null && input.value !== "") {
                     // remove the id prefix
                     // _id = input.id.split(id + "_")[1]
                     if (input.type === "checkbox") {
                         value = input.checked
                     } else {
                         value = parseFloat(input.value)
                         if (isNaN(value)) {
                             value = input.value
                         }
                     }
                     json[key][input.name] = value
                 }
             }
        }

    }

    return json
}

function updateAdditionalForms(json) {
    let additional_options = ["label_to_edges_kwargs", "detect_foreground_kwargs", "robust_invert_kwargs"]

    for (let key in additional_options) {
        key = additional_options[key]
        id = "additional_options_" + key
        let form = document.getElementById(id)
        if (!form) {
            continue
        }
        form.hidden = !(key in json);
        form.needsValidation = form.hidden
    }
}

var _json
var _original_json

function get_json() {
    json = _json
    json = updateJsonWithForm(json)
    //return a copy of the json
    return JSON.parse(JSON.stringify(json))
}

function set_json(json) {
    updateFormWithJson(json.experiment["config"])
    updateImageFields(json.experiment)
    updateAdditionalForms(json)
    _json = json
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
                    _original_json = JSON.parse(json)

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
            document.getElementById("selectImages").disabled = false;
            document.getElementById("viewButton").disabled = true;
            document.getElementById("runButton").classList.replace("btn-outline-primary", "btn-primary")
            document.getElementById("selectImages").classList.replace("btn-primary", "btn-secondary")
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
        document.getElementById("runButton").disabled = false;
        document.getElementById("selectImages").disabled = false;
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
        json = _original_json
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
