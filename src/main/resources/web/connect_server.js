let PORT = undefined;

function do_fetch() {
    fetch('http://127.0.0.1:' + PORT + '/config/available')
        .then(response => {
            // Check if the request was successful
            if (response.ok) {
                return response.json();
            }
        })
        .then(data => {
            postMessage(data)
        }).catch(error => {
            setTimeout("do_fetch()",1000);
        })
}

onmessage = function(e) {
    PORT = e.data;
    do_fetch();
};
