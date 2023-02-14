const DiskMode = [
	{Id:"ReadOnly", value:"Read Only"},
	{Id:"ReadWrite", value:"Read/Write"},
	{Id:"Maintenance", value:"Maintenance"}
];

export function getDiskMode() {
	return DiskMode;
}
export function getDiskModeToValue(value)
{
	switch (value){
		case "Read Only": return "ReadOnly";
		case "Read/Write": return "ReadWrite";
		case "Maintenance": return "Maintenance";
	}
}
