const baseUrl = "/api/v1/catalog/";
const pageSize = 25;
const ItemTypes = { ComponentNodes: 'box', Nodes: 'node' };
const notifyTextLimit = 90;
const toastOpt = { timeOut:0, closeButton:true, tapToDismiss:false, extendedTimeOut:0 };
const PieChartColor = ["#006ea0", "#77b0bd", "#b7cfdb", "#9dd1e9"];
let deleteNodeIdArr =[];

export {
	baseUrl,
	pageSize,
	ItemTypes,
  notifyTextLimit,
  toastOpt,
  PieChartColor,
  deleteNodeIdArr
};
