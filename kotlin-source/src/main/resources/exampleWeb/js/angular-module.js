"use strict";

const app = angular.module('demoAppModule', ['ui.bootstrap']);

// Fix for unhandled rejections bug.
app.config(['$qProvider', function ($qProvider) {
    $qProvider.errorOnUnhandledRejections(false);
}]);

app.controller('DemoAppController', function($http, $location, $uibModal) {
    const demoApp = this;

    // We identify the node.
    const apiBaseURL = "/api/example/";
    let peers = [];

    $http.get(apiBaseURL + "me").then((response) => demoApp.thisNode = response.data.me);

    $http.get(apiBaseURL + "peers").then((response) => peers = response.data.peers);

    demoApp.openModal = () => {
        const modalInstance = $uibModal.open({
            templateUrl: 'demoAppModal.html',
            controller: 'ModalInstanceCtrl',
            controllerAs: 'modalInstance',
            resolve: {
                apiBaseURL: () => apiBaseURL,
                peers: () => peers
            }
        });

        modalInstance.result.then(() => {}, () => {});
    };

    demoApp.orders = [];
    demoApp.getOrders = () => $http.get(apiBaseURL + "orders")
        .then(function(result) {
                  demoApp.orders = result.data;
              });

    demoApp.getOrders();

    // Transfer Order.
    demoApp.transfer = () => {
         const transferOrderEndpoint =
             apiBaseURL +
             "transfer-bl";


        // Create PO and handle success / fail responses.
        $http.put(transferOrderEndpoint, angular.toJson(demoApp.selectedOrder.ref)).then(
              (result) => demoApp.displayMessage(result),
              (result) => demoApp.displayMessage(result)
        );
    };

    demoApp.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

});

app.controller('ModalInstanceCtrl', function ($http, $location, $uibModalInstance, $uibModal, apiBaseURL, peers) {
    const modalInstance = this;

    modalInstance.peers = peers;
    modalInstance.form = {};
    modalInstance.formError = false;
    modalInstance.selectedOrder = null;

    // Validate and create BL.
    modalInstance.create = () => {
        if (invalidFormInput()) {
            modalInstance.formError = true;
        } else {
            modalInstance.formError = false;

            const order = {
                referenceNumber: modalInstance.form.referenceNumber,
                totalAmount: modalInstance.form.totalAmount,
            };

            $uibModalInstance.close();

            const createOrderEndpoint =
                apiBaseURL +
                modalInstance.form.seller +
                "/" +
                modalInstance.form.buyer +
                "/create-order";

            // Create PO and handle success / fail responses.
            $http.put(createOrderEndpoint, angular.toJson(bl)).then(
                (result) => modalInstance.displayMessage(result),
                (result) => modalInstance.displayMessage(result)
            );
        }
    };

    modalInstance.displayMessage = (message) => {
        const modalInstanceTwo = $uibModal.open({
            templateUrl: 'messageContent.html',
            controller: 'messageCtrl',
            controllerAs: 'modalInstanceTwo',
            resolve: { message: () => message }
        });

        // No behaviour on close / dismiss.
        modalInstanceTwo.result.then(() => {}, () => {});
    };

    // Close create Order modal dialogue.
    modalInstance.cancel = () => $uibModalInstance.dismiss();

    // Validate the Order.
    function invalidFormInput() {
        return (modalInstance.form.referenceNumber === undefined) || (modalInstance.form.seller === undefined) || (modalInstance.form.buyer === undefined);
    }
});

// Controller for success/fail modal dialogue.
app.controller('messageCtrl', function ($uibModalInstance, message) {
    const modalInstanceTwo = this;
    modalInstanceTwo.message = message.data;
});