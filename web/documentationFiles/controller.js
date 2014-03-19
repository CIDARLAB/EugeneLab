  $( document ).ready(function() { // loads once the document is ready
    
    // the variables that will hold all of the content
    var $downloadRun = $('<div>'); 
    var $syntax = $('<div>');
    var $dataio = $('<div>');
    var $domainConcepts = $('<div>');
    var $controlFlow = $('<div>');
    var $dynamicNaming = $('<div>');
    var $rules = $('<div>');
    var $preFunctions  = $('<div>');
    var $combinatorialFunctions = $('<div>');
    var $directionality = $('<div>');
    var $invertase = $('<div>');
    var $regulatoryInteractions = $('<div>');
    var $vis = $('<div>');
    var $example = $('<div>');
    var $tools = $('<div>');
    var $publications = $('<div>');


    // loading the html into the variables
    $downloadRun.load("documentationFiles/downloadRun.html",function() { 
    console.log("Loaded down");
    });

    $syntax.load("documentationFiles/syntax.html",function(){
      console.log("Loaded syntax")
    });

    $dataio.load("documentationFiles/dataio.html",function(){
      console.log("Loaded dataio")
    });

    $domainConcepts.load("documentationFiles/domainConcepts.html",function(){
      console.log("Loaded domainConcepts")
    });

    $controlFlow.load("documentationFiles/controlFlow.html",function(){
      console.log("Loaded controlFlow")
    });

    $dynamicNaming.load("documentationFiles/dynamicNaming.html",function(){
      console.log("Loaded dynamicNaming")
    });

    $rules.load("documentationFiles/rules.html",function(){
      console.log("Loaded rules")
    });

    $preFunctions.load("documentationFiles/preFunctions.html",function(){
      console.log("Loaded rules")
    });

    $combinatorialFunctions.load("documentationFiles/combinatorialFunctions.html",function(){
      console.log("Loaded rules")
    });  

    $directionality.load("documentationFiles/directionality.html",function(){
      console.log("Loaded directionality")
    });    

    $invertase.load("documentationFiles/invertase.html",function(){
      console.log("Loaded invertase")
    });  

    $regulatoryInteractions.load("documentationFiles/regulatoryInteractions.html",function(){
      console.log("Loaded regulatoryInteractions")
    }); 

    $vis.load("documentationFiles/visualization.html",function(){
      console.log("Loaded regulatoryInteractions")
    }); 

    $example.load("documentationFiles/examples.html",function(){
      console.log("Loaded example")
    }); 

    $tools.load("documentationFiles/tools.html",function(){
      console.log("Loaded tools")
    }); 

    $publications.load("documentationFiles/publications.html",function(){
      console.log("Loaded publications")
    }); 


    $("#content").html($downloadRun); // loads the initial content


    // the various click events that will switch tabs and data
    $("#downloadRun").click(function(){ 
     $("#content").html($downloadRun);
     $(".active").removeClass('active');
     $("#downloadRun").addClass('active');
     $( window ).scrollTop(0);
    });


    $("#basic_syntax").click(function(){ 
     $("#content").html($syntax);
     $(".active").removeClass('active');
     $("#basic_syntax").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#dataio").click(function(){ 
     $("#content").html($dataio);
     $(".active").removeClass('active');
     $("#dataio").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#domainConcepts").click(function(){ 
     $("#content").html($domainConcepts);
     $(".active").removeClass('active');
     $("#domainConcepts").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#controlFlow").click(function(){ 
     $("#content").html($controlFlow);
     $(".active").removeClass('active');
     $("#controlFlow").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#dynamicNaming").click(function(){ 
     $("#content").html($dynamicNaming);
     $(".active").removeClass('active');
     $("#dynamicNaming").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#rules").click(function(){ 
     $("#content").html($rules);
     $(".active").removeClass('active');
     $("#rules").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#preFunctions").click(function(){ 
     $("#content").html($preFunctions);
     $(".active").removeClass('active');
     $("#preFunctions").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#combinatorialFunctions").click(function(){ 
     $("#content").html($combinatorialFunctions);
     $(".active").removeClass('active');
     $("#combinatorialFunctions").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#directionality").click(function(){ 
     $("#content").html($directionality);
     $(".active").removeClass('active');
     $("#directionality").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#invertase").click(function(){ 
     $("#content").html($invertase);
     $(".active").removeClass('active');
     $("#invertase").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#regulatoryInteractions").click(function(){ 
     $("#content").html($regulatoryInteractions);
     $(".active").removeClass('active');
     $("#regulatoryInteractions").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#vis").click(function(){ 
     $("#content").html($vis);
     $(".active").removeClass('active');
     $("#vis").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#example").click(function(){ 
     $("#content").html($example);
     $(".active").removeClass('active');
     $("#example").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#tools").click(function(){ 
     $("#content").html($tools);
     $(".active").removeClass('active');
     $("#tools").addClass('active');
     $( window ).scrollTop(0);
    });

    $("#publications").click(function(){ 
     $("#content").html($publications);
     $(".active").removeClass('active');
     $("#publications").addClass('active');
     $( window ).scrollTop(0);
    });



  });