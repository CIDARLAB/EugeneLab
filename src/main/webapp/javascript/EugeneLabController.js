/* Gets a list of images from the server and adds them to the imageList
 * Also adds a unique id to each part, viewable when clicked
 */


$(document).ready(function() {
    var deviceCount = 0;
    var _properties = {};
    var _partTypes = {};
    var _parts = {}; //key: name, value: part JSON
    var _partIds = {}; //key: name, value: uuid
    var _newParts = {}; //key: name, value: part JSON

    
    /********Functions********/
    //load files list
    var savePart = function(part, partId) {
//        send("create", JSON.stringify(part));
        if (partId === undefined) {
            partId = new ObjectId().toString();
        }
        var seqId = new ObjectId().toString();
        if (part["schema"] === "BasicPart") {
            //save basic part
            if (_partIds[part["name"]] === undefined) {
                _parts[part["name"]] = part;
                _partIds[part["name"]] = partId;
                if (_connection.readyState === 1) {
                    send("create", JSON.stringify({
                        schema: "BasicPart",
                        _id: partId,
                        author: "51e9579344ae846673a51b0f", //this probably shouldnt be hard coded later...
                        shortDescription: part["pigeon"],
                        sequence: {
                            _id: seqId,
                            isRNA: false,
                            isSingleStranded: false,
                            sequence: part["sequence"],
                            isDegenerate: false,
                            isLinear: false,
                            isCircular: false,
                            isLocked: false
                        },
                        name: part["name"],
                        format: "FreeForm",
                        type: part["type"],
                        riskGroup: 0,
                    }
                    ));
                }
            }
        } else if (part["schema"] === "CompositePart") {
            if (_partIds[part["name"]] === undefined) {
                //create new part if it doesn't exist already
                _parts[part["name"]] = part;
                _partIds[part["name"]] = partId;

                //gather the composition and sequence of the composite part
                var composition = [];
                var compositeSequence = "";
                for (var i = 0; i < part["components"].length; i++) {
                    var basicPart = part["components"][i];
                    var basicPartId = _partIds[basicPart.name];
                    if (basicPartId === undefined) {
                        //create new basic parts
                        basicPartId = new ObjectId().toString();
                        var basicSeqId = new ObjectId().toString();

                        _parts[basicPart["name"]] = basicPart;
                        _partIds[basicPart["name"]] = basicPartId;
                        if (_connection.readyState === 1) {
                            send("create", JSON.stringify({
                                schema: "BasicPart",
                                _id: basicPartId,
                                author: "51e9579344ae846673a51b0f", //this probably shouldnt be hard coded later...
                                shortDescription: basicPart["pigeon"],
                                sequence: {
                                    _id: basicSeqId,
                                    isRNA: false,
                                    isSingleStranded: false,
                                    sequence: basicPart["sequence"],
                                    isDegenerate: false,
                                    isLinear: false,
                                    isCircular: false,
                                    isLocked: false
                                },
                                name: basicPart["name"],
                                format: "FreeForm",
                                type: basicPart["type"],
                                riskGroup: 0,
                            })
                                    );
                        }

                    }
                    composition.push(basicPartId);
                    compositeSequence = compositeSequence + basicPart.sequence;
                }

                //create composite part
                if (_connection.readyState === 1) {
                    send("create", JSON.stringify({
                        schema: "CompositePart",
                        _id: partId,
                        author: "51e9579344ae846673a51b0f", //this probably shouldnt be hard coded later...
                        shortDescription: part["pigeon"],
                        sequence: {
                            _id: seqId,
                            isRNA: false,
                            isSingleStranded: false,
                            sequence: compositeSequence,
                            isDegenerate: false,
                            isLinear: false,
                            isCircular: false,
                            isLocked: false
                        },
                        name: part["name"],
                        composition: composition,
                        format: "FreeForm",
                        type: "composite",
                        riskGroup: 0,
                    }
                    ));
                }

            }
        } else {
//            probably not a part at all
        }
    };

    var drawPartsList = function(data) {
        var drawn = {};
        var toAppend = '<table class="table table-bordered table-hover" id="partsList"><thead><tr><th>Name</th><th>Type</th></tr></thead><tbody>';
        if (data !== undefined) {
            $.each(data, function() {
                if (drawn[this["name"]] === undefined) {
                    if (this["type"] === undefined) {
                        this["type"] = "gene";
                    }
                    toAppend = toAppend + '<tr><td>' + this["name"] + '</td><td>' + this["type"] + '</td></tr>';
                    _parts[this["name"]] = this;
                    _partIds[this["name"]] = this['id'];
                    drawn[this["name"]] = "added";
                }
            });
        }
        $.each(_parts, function() {
            if (drawn[this["name"]] === undefined) {
                if (this["type"] === undefined) {
                    this["type"] = "gene";
                }
                toAppend = toAppend + '<tr><td>' + this["name"] + '</td><td>' + this["type"] + '</td></tr>';
                drawn[this["name"]] = "added";

            }
        });
        toAppend = toAppend + "</tbody></table>";
        $('#partsListArea').html(toAppend);
        $("#partsList").dataTable({
            "bPaginate": false,
            "bScrollCollapse": true
        });
        $('tr').dblclick(function() {
            var type = $(this).children("td:last").text();
            var originalName = $(this).children("td:first").text();
            var name = originalName.replace(/\s+/g, '');
            if (_partTypes[type] === undefined) {
                _partTypes[type] = "added";
                newLine = 'PartType ' + type + '(Name, Sequence);\n' + newLine;
            }
            var sequence = "";
            if (typeof _parts[originalName].sequence === "string") {
                sequence = _parts[name].sequence;
            } else {
                if (_parts[originalName].sequence === undefined) {
                    sequence = "";
                } else {
                    sequence = _parts[originalName].sequence.sequence;
                }
            }
            var pigeon = 'p ' + name + ' 10';
            var newLine = type + ' ' + name + '("' + originalName + '","' + pigeon + '");\n';
            var line = editor.getCursor("start")["line"];
            var currentLine = editor.getLine(line);
            editor.setLine(line, newLine + currentLine);
        });
    };

    var loadLibraryTree = function() {
        $("#libraryArea").html("");

        $.get("EugeneLabServlet", {"command": "getLibrary"}, function(response) {

            $("#libraryArea").dynatree({
                onActivate: function(node) {
                    // A DynaTreeNode object is passed to the activation handler
                    // Note: we also get this event, if persistence is on, and the page is reloaded.
//                alert("You activated " + node.data.title);
                },
                onDblClick: function() {
                    loadFile();
                },
                persist: false,
                children: response
            });
            
            $('#libraryArea').dynatree("getTree").reload();
        });
    };
    
    //draw the file table tree based on users file contents
    var loadFileTree = function() {
        $("#filesArea").html("");
        $.get("EugeneLabServlet", {"command": "getFileList"}, function(data) {
            var children = data;
            $("#filesArea").dynatree({
                onActivate: function(node) {
                    // A DynaTreeNode object is passed to the activation handler
                    // Note: we also get this event, if persistence is on, and the page is reloaded.
//                alert("You activated " + node.data.title);
                },
                onDblClick: function() {
                    loadFile();
                },
                persist: false,
                children: children
            });
            $('#filesArea').dynatree("getTree").reload();
        });
    };

    var getActiveNode = function() {
        return $("#filesArea").dynatree("getActiveNode");
    };

    // Return the active node's file extension
    var getActiveNodeExtension = function() {
        var node = getActiveNode();
        
        // no node selected... 
        // use root ...
        if(node === null) {
        	return "/";
        }
        
        var nodeName = node.data.title;
        var parent = node.getParent();
        while (parent.data.title !== null) {
            nodeName = parent.data.title + "/" + nodeName;
            parent = parent.getParent();
        }
        if (node.data.isFolder) {
            nodeName += "/";
        }
        return nodeName;
    };

    var addNewFolder = function(newFolderName) {
        var activeFolder = getActiveNodeExtension();
        var newFolderExtension = activeFolder + newFolderName + "/";
        var command = {"command": "createFolder", "extension": newFolderExtension};
        $.post("EugeneLabServlet", command, function(response) {
            var isSuccessful = response["isSuccessful"];
            alert(JSON.stringify(response));
            if (isSuccessful) {
                getActiveNode().addChild({
                    title: newFolderName,
                    isFolder: true
                });
            } else {
                alert("Folder name already exists");
            }
        });
    };

    //save files
    var saveFile = function(newFileName) {
        $('#fileName').text(newFileName);
        var fileContent = editor.getValue();
        //create new file on server
        $.post("EugeneLabServlet", {"command": "saveFileContent", "fileName": newFileName, "fileContent": fileContent}, function(response) {
            var rootNode = $("#filesArea").dynatree("getRoot");
            // Call the DynaTreeNode.addChild() member function and pass options for the new node
            var childNode = rootNode.addChild({
                title: newFileName,
                isFolder: false
            });
            //
            $('#newFileNameInput').val("");
        });
    };

    var getFileType = function() {
        var fileName = $('#fileName').text();
        var index = fileName.lastIndexOf('.');
        var fileType = fileName.substring(index + 1);
        return fileType;

    };


    var currentFileExtension;
    var setCurrentFileExtension = function(fileExtension) {
        currentFileExtension = fileExtension;
    };
    var getCurrentFileExtension = function() {
        return currentFileExtension;
    };

    var loadFile = function() {
        var node = $("#filesArea").dynatree("getActiveNode");
        if (node.data.isFolder) {
            //do nothing i guess...
        } else {
            var fileName = node.data.title;
            setCurrentFileExtension(getActiveNodeExtension());
            $('#fileName').text(fileName);
            var parent = node.getParent();
            while (parent.data.title !== null) {
                fileName = parent.data.title + "/" + fileName;
                parent = parent.getParent();
            }
            $.get("EugeneLabServlet", {"command": "loadFile", "fileName": fileName}, function(response) {
                editor.setValue(response);
            });
        }
    };

    //Event Handlers
    $('#refreshButton').click(function() {
        var refreshType = $('ul#leftTabHeader li.active').text();
        if (refreshType === "Parts") {
            if (_connection.readyState === 1) {
                send("query", '{"schema":"BasicPart"}', function(data) {
                    drawPartsList(data);
                });
            } else {
                drawPartsList();
            }

        } else {
            //refresh files
            loadFileTree();
            loadLibraryTree();
        }
    });

    $('#uploadFileButton').click(function() {
        var newFileName = $('#file').val();
        if (newFileName !== "") {
            $('#uploadForm').submit();
        } else if ($('a.dynatree-title:contains("' + newFileName + '")').length === 0) {
            editor.setValue("");
            saveFile(newFileName);
        }
    });
    
    
    $('#createNewFileButton').click(function() {
        var filename = $('#newFileNameInput').val();
        if (filename !== "") {
        	
        	var folder = "";
            var node = getActiveNode();
            alert(node);
            if(node !== null) {
            	if(node.hasChildren() === true) {
            		folder = node;
            	} else {
            		folder = node.getParent();
            	}
            }
            
            var command = {"command": "createFile", "filename":filename, "folder": folder};
            $.post("EugeneLabServlet", command, function(response) {
            	if(response["exception"] !== null) {
            		alert(response["exception"]);
            	} else {
            		// if everything went fine, 
            		// add the file name to the file tree
            		//activeFolder.addChild({title: newFileName});
            	}
            });
        } else if ($('a.dynatree-title:contains("' + filename + '")').length === 0) {
            editor.setValue("");
            saveFile(filename);
        }
    });
    
    
    $('#saveButton').click(function() {
        var newFileName = $('#fileName').text();
        if ($('a.dynatree-title:contains("' + newFileName + '")').length === 0) {
            saveFile(newFileName);
        }
    });



    $('#deleteModalButton').click(function() {
        var node = $("#filesArea").dynatree("getActiveNode");
        if(node !== null) {
            var fileName = node.data.title;
            $('#toDeleteName').html("Do you really want to delete <strong>" + fileName + "</strong>?");
        }
    });

    
    $('#yesDeleteFileButton').click(function() {
        
        var activeFolder = getActiveNodeExtension();
    	var node = $("#filesArea").dynatree("getActiveNode");
        
        var command = {"command": "deleteFile", "filename":node, "folder": activeFolder};
        $.post("EugeneLabServlet", command, function(response) {
        	if(response["exception"] !== null) {
        		alert(response["exception"]);
        	} else {
        		// if everything went fine, 
        		// then remove the active node from the file tree
        		node.remove();
        	}
        });
        
    });

    $('#loadButton').click(function() {
        loadFile();
    });

    $('#deleteButton').click(function() {
        var node = $("#filesArea").dynatree("getActiveNode");
        if (node.data.isFolder) {
            //do nothing i guess...
        } else {
            var fileName = node.data.title;
            var parent = node.getParent();
            while (parent.data.title !== null) {
                fileName = parent.data.title + "/" + fileName;
                parent = parent.getParent();
            }
            $.get("EugeneLabServlet", {"command": "deleteFile", "fileName": fileName}, function() {
                node.remove();
            });
        }
    });

    $('#newDeviceButton').click(function() {
        if (deviceCount === 0) {
            $('#spectaclesEditorArea button').remove();
            $('#trash').droppable({
                hoverClass: "droppable-hover",
                drop: function(event, ui) {
                    var element = ui.draggable.css('position', '');
                    if (element.hasClass('blank')) {
                        element.parent().remove();
                    }
                    if (!element.hasClass("partIcon")) {
                        $(this).append(element);
                        $(ui.draggable).fadeOut(500);
                    }
                }}
            );
        }
        $('#spectaclesEditorArea').append('<ul id="device' + deviceCount + '" class="device droppable sortable"><li class="blank" style="vertical-align:bottom;height:80px;width:80px;border:1px solid grey" title="Drag a part here to get started">New Design</li></ul>');
        $("#device" + deviceCount).sortable({
            revert: true
        });
        $("#device" + deviceCount).droppable({
            drop: function() {
                $(this).droppable("destroy");
                var firstItem = $(this).find("li.blank");
                firstItem.html('<strong>' + $(this).attr("id") + '</strong>');
                firstItem.addClass("notSortable");
                $(this).sortable({
                    revert: true,
                    items: "li:not(.notSortable)",
                    connectWith: "#trash",
                    stop: function(event, ui) {
                        var firstItem = $(ui.item).parent().find("li.blank");
                        $(ui.item).parent().prepend(firstItem);
                        ui.item.removeClass('partIcon');
                    }
                });
                firstItem.attr("title", "Select to edit device properties");
                $(this).prepend(firstItem);
                firstItem.attr("style", 'height:40px;width:80px;background:#0081c2;vertical-align:top;padding:5px');
                firstItem.addClass("rotatedText");
                firstItem.click(function() {
                    $('.selected').removeClass("selected");
                    $(this).addClass("selected");
                });
            }
        });
        deviceCount = deviceCount + 1;
    });

    var progress = 0;
    var timer = setInterval(updateProgressbar, 10);
    

    //$('#runButton') jquery syntax
    //assigns various functions to the run button when clicked
    $('#runButton').click(function() {
        //var outputMessageString = '<p> Status: </p>';
        //$('#outputMessage').html(outputMessageString);
        $('#outputArea').collapse('show');

        /*
         * start the timer
         */
        
        if ($('div#textEditorTab').hasClass("active")) {
            text = true;
        }

        if (!text) {
        	/***  FOR GRAPHICAL EDITOR --- Spectacles Web-Version
            var command = {};
            command["command"] = "run";
            command["devices"] = "";
            command["parts"] = [];
            var devices = "";
            $('#spectaclesEditorArea ul').each(function() {
                if ($(this).find("li").length > 1) {
                    var device = "Device " + $(this).attr("id") + "(";
                    var count = 0;
                    $(this).find("li").each(function() {
                        if (count > 0) {
                            device = device + $(this).attr("title").split(' ').join("") + ",";
                        }
                        count = count + 1;
                    });
                    device = device.substring(0, device.length - 1);
                    device = device + ")";
                    devices = devices + device + "|";
                }
            });
            devices = devices.substring(0, devices.length - 1);
            command["devices"] = devices;
            
            alert(command["devices"]);
            //window.location.replace("http://cidar.bu.edu/ravencad/ravencad.html");
            $.get("EugeneLabServlet", command, function(response) {
                alert(response);
            });
            ***/
        } else {
            //Clicking run button sends current text to server
            //May want to modify to send file or collection of files to server(if Eugene program spans multiple files)
            $('#runButton').attr("disabled", "disabled");
            var command = {"script": editor.getValue(), "command": "execute"};
            
            /*** WE ONLY SUPPORT EUGENE SCRIPTS 
            // Get file type to determine command
            var fileType = getFileType();
            var fileExtension = getCurrentFileExtension();
            // Command is based on the file type
            if (fileType === 'eug') {
            	command = {"input": editor.getValue(), "command": "execute"};
            } else if (fileType === 'sbol') {
                command = {"input": fileExtension, "command": "executeSBOL"};
            } else if (fileType === 'gbk' || fileType === 'gb') {
                command = {"input": fileExtension, "command": "executeGenBank"};
            } else {
                command = {"input": editor.getValue(), "command": "execute"};
            }
			***/
            
            $.post("EugeneLabServlet", command, function(response) {
            	
            	$('#runButton').removeAttr("disabled");

            	//alert(response["status"]);
                if ("good" === response["status"]) {
                    
                	// clear the output message box
                    $('#outputMessage').html('');

                    /*
                     * PIGEON
                     */                	
                    if (response["pigeon-uri"] !== undefined) {
//                        var pigeonLinks = [];
                        var imageHeader = '<div id="outputCarousel" class="slide carousel"><ol class="carousel-indicators">';
                        var images = '<div class="carousel-inner">';
                        var imageCount = 0;
                        
//                        $.each(response["results"], function() {
//                            pigeonLinks.push(this["pigeon-uri"]);
                            
                            var active = "";
                            if (imageCount === 0) {
                                active = "active";
                            }
                            imageHeader = imageHeader + '<li class="' + active + '" data-target="#outputCarousel" +data-slide-to="' + imageCount + '"></li>';
                            images = images + '<div class="item ' + active + '"><img src="' + response["pigeon-uri"] + '"/><div class="carousel-caption"><h4>' + this["name"] + '</h4></div></div>';
                            imageCount++;
//                        }
//                        );
                        //render images
                        imageHeader = imageHeader + '</ol>';
                        images = images + '</div><a class="carousel-control left" href="#outputCarousel" data-slide="prev">&lsaquo;</a> <a class="carousel-control right" href="#outputCarousel" data-slide="next">&rsaquo;</a></div>';
                        var slideShow = imageHeader + images;
                        
                        $('#outputImageArea').html('<img src=".' + response["pigeon-uri"] + '"/>');
                        
                        //$('#outputImageArea').html(slideShow);
                        //$('#outputCarousel').carousel({interval: 5000});
                        //render new parts list
                        var toAppend = '<table class="table table-bordered table-hover" id="outputList"><thead><tr><th>Name</th><th>Type</th><th></th></tr></thead><tbody>';
                        //handle each device
                        var newParts = {};

/*                        
                        $.each(response["results"], function() {
                            if (newParts[this["name"]] !== "added") {
                                _newParts[this["name"]] = this;
                                toAppend = toAppend + '<tr><td>' + this["name"] + '</td><td>' + this["type"] + '</td><td><button class="btn btn-success savePartButton">Save</button></td></tr>';
                                //handle each component
                                $.each(this["components"], function() {
                                    if (this["type"] !== undefined && newParts[this["name"]] !== "added") {
                                        _newParts[this["name"]] = this;
                                        toAppend = toAppend + '<tr><td>' + this["name"] + '</td><td>' + this["type"] + '</td><td><button class="btn btn-success savePartButton">Save</button></td></tr>';
                                        newParts[this["name"]] = "added";
                                    }
                                });
                                newParts[this["name"]] = "added";
                            }
                        });
 */
                        toAppend = toAppend + "</tbody></table>";
                        $('#outputListArea').html(toAppend);
                        $("#outputList").dataTable({
                            "bPaginate": false,
                            "sScrollY": "300px"
                        });
                        $('#outputListArea').parent().append('<button class=btn btn-large btn-success" id="saveAllButton">Save All Parts</button>');
                        $('.savePartButton').click(function() {
                            //save a part
                            savePart(_newParts[$(this).parent().parent().children("td:first").text()]);
                            $(this).text("Saved");
                            $(this).addClass("disabled");
                        });
                        $('#saveAllButton').click(function() {
                            $('#outputList tr').each(function(i) {
                                if (i > 0) {
                                    savePart(_newParts[$(this).children('td:first').text()]);
                                }
                            });
                            $('.savePartButton').text("Saved");
                            $('.savePartButton').addClass("disabled");
                            $(this).text("All Parts Saved");
                            $(this).addClass("disabled");
                        });
                        $('#outputArea').collapse('show');
                        drawPartsList();
                    }
                }
                else if ("exception" === response["status"]) {
                	
                	// print the exception
                    $('#outputMessage').html("<font color=red>Exception: " + response["reason"] + "</font>");
                    
                    // delete the content of all other output areas
                    $('#outputImageArea').html('');
                    $('#outputListArea').html('');

                }
            });
        }
    });

    /*
     * LOAD the list of files and the library of the current user
     */
    loadLibraryTree();
    loadFileTree();


    /********Clotho Functions and Variables --- WILL BE UPDATED AS SOON AS CLOTHO IS UP AND RUNNING!
    var _connection = new WebSocket('wss://localhost:8443/websocket');

    var _requestCommand = {}; //key request id, value: callback function
    var _requestID = 0;


    var send = function(channel, data, callback) {
        if (_connection.readyState === 1) {
            var message = '{"channel":"' + channel + '", "data":' + data + ',"requestId":"' + _requestID + '"}';
            _requestCommand[channel + _requestID] = callback;
            _connection.send(message);
            _requestID++;
        } else {
            _connection = new WebSocket('wss://localhost:8443/websocket');
        }
    };
    _connection.onmessage = function(e) {
        //parase message into JSON
        var dataJSON = $.parseJSON(e.data);
        //ignore say messages which have not requestId
        var channel = dataJSON["channel"];
        var requestId = dataJSON["requestId"];
        if (requestId !== null) {
            //if callback function exists, run it
            var callback = _requestCommand[channel + requestId];
            if (callback !== undefined) {
                callback(dataJSON["data"]);
                delete _requestCommand[channel + requestId];
            }
        }
    };

    _connection.onerror = function(e) {
//        alert("connection error: is clotho running?");
    };

    _connection.onclose = function() {
//        alert('closing connection');
    };

    _connection.onopen = function(e) {
        if (_connection.readyState === 1) {
            send("query", '{"schema":"BasicPart"}', function(data) {
                drawPartsList(data);
            });
        }
    };
********/

    //functions to run on page load

    var editor = CodeMirror.fromTextArea(document.getElementById("textEditor"), {
        styleActiveLine: true,
        lineNumbers: true,
        lineWrapping: true,
        theme: "neat",
        mode: "eugene"
    });
    
    var command = {"command": "imageList"};
    // Get the JSON object with the location of the images
    // JSON has key imageList with a value as a array of JSON objects with a single key "location"

    $.get("EugeneLabServlet", command, function(response) {
        var i = 0;
        $('#iconArea').html("");
        $.each(response, function() {
            var type = this["fileName"].split("\.")[0];
            $("#iconArea").append('<div class="span5"><li class="draggable partIcon" title= "' + type.replace(/-/g, ' ') + '" id="' + type + '"><div class="thumbnail"><img class="img-rounded" style="width:40px;height:80px" src="images/sbol_visual_jpeg/' + this["fileName"] + '"></div></li></div>');
            $('#' + type).dblclick(function() {
                var newValue = editor.getValue();
                var type = $(this).attr('id');
                if (_partTypes[type] === undefined) {
                    _partTypes[type] = "added";
                    newValue = 'PartType ' + type + '(Name, Sequence);\n' + newValue;
                }
                editor.setValue(newValue);
            });
            i = i + 1;
        });
        $('#iconArea .draggable').draggable({
            helper: "clone",
            connectToSortable: ".sortable, #trash",
            revert: "invalid"
        });
        $('#iconArea li').on("click", function() {
            $(".selected").removeClass("selected");
            $(this).parent().addClass("selected");
        });
    });

    function updateProgressbar(){
        $("#progressbar").progressbar({
            value: ++progress
        });
        if(progress == 100)
            clearInterval(timer);
    }

    $(function () {
        $("#progressbar").progressbar({
            value: progress
        });
    });    
});

